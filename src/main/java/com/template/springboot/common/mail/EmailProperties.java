package com.template.springboot.common.mail;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * App-level email settings. SMTP host/port/credentials are bound to Spring Boot's
 * {@code spring.mail.*} properties (so {@link org.springframework.mail.javamail.JavaMailSender}
 * is auto-configured) — this block only covers concerns the app owns: the visible "From",
 * the public URL embedded in template links, the master enabled switch, and per-flow TTLs.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.mail")
public class EmailProperties {

    /** Master switch. When false, EmailService.send() becomes a no-op (logs only) and the
     *  forgot-password flow rejects requests with 503 so misconfigured envs fail loudly. */
    private boolean enabled = true;

    /** "From" address. Many providers (Gmail, Office365) require this to match the SMTP login. */
    private String from;

    /** Human-readable name shown before the address in mail clients. */
    private String fromName = "Spring Boot Template";

    /** Public base URL used to build links inside templates ({@code {{appUrl}}}). */
    private String appUrl = "http://localhost:8080";

    /** How long a password-reset token stays valid. */
    private Duration passwordResetTtl = Duration.ofMinutes(30);
}
