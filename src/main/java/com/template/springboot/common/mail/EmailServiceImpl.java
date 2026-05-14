package com.template.springboot.common.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Default {@link EmailService} implementation, wired on top of Spring's auto-configured
 * {@link JavaMailSender}.
 *
 * <p>Works with any SMTP provider that supports STARTTLS or SSL — Gmail, Outlook/Office365,
 * Yahoo, SendGrid, Mailgun, Postmark, AWS SES, self-hosted Postfix, etc. Provider selection
 * is configured entirely via env vars; see {@code .env.example}.
 *
 * <p>When {@code app.mail.enabled=false} or no {@link JavaMailSender} bean is present
 * (e.g. {@code spring.mail.host} unset), {@link #send} logs a warning and returns without
 * throwing. Callers can opt to check {@link #isEnabled()} first if they need to short-circuit
 * dependent business logic.
 *
 * <p>Sends are {@code @Async}, so the calling request is never blocked on SMTP latency.
 * Wire a dedicated {@code mailExecutor} bean if you want isolation from the audit executor;
 * by default Spring's shared default executor is used.
 */
@Service
@EnableConfigurationProperties(EmailProperties.class)
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final EmailProperties properties;
    private final EmailTemplateRenderer renderer;
    private final JavaMailSender mailSender;   // null when spring.mail.host is unset

    public EmailServiceImpl(EmailProperties properties,
                            EmailTemplateRenderer renderer,
                            ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.properties = properties;
        this.renderer = renderer;
        this.mailSender = mailSenderProvider.getIfAvailable();
    }

    @jakarta.annotation.PostConstruct
    void logState() {
        if (isEnabled()) {
            log.info("EmailService ready — from='{}', host configured", properties.getFrom());
        } else {
            log.warn("EmailService disabled at startup — {}", disabledReason());
        }
    }

    @Override
    public boolean isEnabled() {
        return properties.isEnabled() && mailSender != null;
    }

    @Override
    public String disabledReason() {
        if (!properties.isEnabled()) return "app.mail.enabled=false";
        if (mailSender == null)      return "JavaMailSender bean missing — check spring.mail.host";
        return null;
    }

    @Override
    @Async
    public void send(EmailMessage message) {
        if (!isEnabled()) {
            log.warn("Email skipped ({}) — to={} subject='{}'",
                    disabledReason(), message.getTo(), message.getSubject());
            return;
        }
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, StandardCharsets.UTF_8.name());
            helper.setFrom(buildFrom());
            helper.setTo(message.getTo());
            if (message.getCc() != null && !message.getCc().isEmpty()) {
                helper.setCc(message.getCc().toArray(String[]::new));
            }
            if (message.getBcc() != null && !message.getBcc().isEmpty()) {
                helper.setBcc(message.getBcc().toArray(String[]::new));
            }
            helper.setSubject(message.getSubject());
            // setText(text, html) — when both passed, multipart/alternative is built automatically.
            String html = message.getHtmlBody();
            String text = message.getTextBody();
            if (html != null && text != null) {
                helper.setText(text, html);
            } else if (html != null) {
                helper.setText(html, true);
            } else {
                helper.setText(text == null ? "" : text, false);
            }
            mailSender.send(mime);
            log.debug("Email sent to={} subject='{}'", message.getTo(), message.getSubject());
        } catch (MessagingException | UnsupportedEncodingException ex) {
            log.error("Failed to send email to={} subject='{}': {}",
                    message.getTo(), message.getSubject(), ex.getMessage(), ex);
        } catch (org.springframework.mail.MailException ex) {
            log.error("SMTP error sending email to={} subject='{}': {}",
                    message.getTo(), message.getSubject(), ex.getMessage(), ex);
        }
    }

    @Override
    public void sendTemplated(String template, String to, String subject, Map<String, Object> model) {
        Map<String, Object> enriched = new HashMap<>();
        enriched.put("appUrl", properties.getAppUrl());
        enriched.put("fromName", properties.getFromName());
        if (model != null) enriched.putAll(model);
        String html = renderer.render(template, enriched);
        send(EmailMessage.builder()
                .to(to)
                .subject(subject)
                .htmlBody(html)
                .cc(List.of())
                .bcc(List.of())
                .build());
    }

    private InternetAddress buildFrom() throws UnsupportedEncodingException {
        return new InternetAddress(properties.getFrom(), properties.getFromName(), StandardCharsets.UTF_8.name());
    }
}
