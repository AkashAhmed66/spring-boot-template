package com.template.springboot.common.mail;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Provider-agnostic email payload. Build via {@link #builder()} then hand to
 * {@link EmailService#send(EmailMessage)}.
 *
 * <p>If both {@code htmlBody} and {@code textBody} are set, the message is multipart/alternative
 * (clients pick the format they prefer). HTML-only and text-only are both valid.
 */
@Getter
@Builder
public class EmailMessage {

    private final String to;

    private final List<String> cc;

    private final List<String> bcc;

    private final String subject;

    /** HTML body — preferred for transactional mail. */
    private final String htmlBody;

    /** Plain-text body. Optional. */
    private final String textBody;
}
