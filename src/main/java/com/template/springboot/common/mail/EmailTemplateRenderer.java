package com.template.springboot.common.mail;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads HTML templates from {@code classpath:templates/email/<name>.html} and substitutes
 * {@code {{placeholder}}} tokens with values from a model map. Templates are read once
 * and cached.
 *
 * <p>Intentionally minimal — no Thymeleaf dependency. Sufficient for transactional mail
 * (one-shot links, status updates). For richer templating (loops, conditionals), wire
 * Thymeleaf in and replace this class — the {@link EmailService} signature does not change.
 */
@Component
public class EmailTemplateRenderer {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([\\w.]+)\\s*}}");

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    /**
     * Render template {@code name} (relative to {@code templates/email/}, without {@code .html})
     * with {@code model}. Unknown placeholders are left in place so they're obvious at runtime.
     */
    public String render(String name, Map<String, Object> model) {
        String raw = cache.computeIfAbsent(name, EmailTemplateRenderer::load);
        Matcher m = PLACEHOLDER.matcher(raw);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            Object value = model == null ? null : model.get(m.group(1));
            m.appendReplacement(out, Matcher.quoteReplacement(value == null ? m.group(0) : Objects.toString(value)));
        }
        m.appendTail(out);
        return out.toString();
    }

    private static String load(String name) {
        String path = "templates/email/" + name + ".html";
        try {
            return StreamUtils.copyToString(
                    new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Email template not found: " + path, e);
        }
    }
}
