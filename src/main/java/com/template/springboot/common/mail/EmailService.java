package com.template.springboot.common.mail;

import java.util.Map;

public interface EmailService {

    /** True when {@code app.mail.enabled=true} AND SMTP host is configured. */
    boolean isEnabled();

    /** Null when {@link #isEnabled()}; otherwise a human-readable reason it's not. */
    String disabledReason();

    /** Send a pre-built message. Async — returns immediately. */
    void send(EmailMessage message);

    /**
     * Convenience: render an HTML template from {@code classpath:templates/email/<template>.html}
     * with the given model, then send it. The model always has the following keys auto-injected
     * unless the caller overrides them: {@code appUrl}, {@code fromName}.
     */
    void sendTemplated(String template, String to, String subject, Map<String, Object> model);
}
