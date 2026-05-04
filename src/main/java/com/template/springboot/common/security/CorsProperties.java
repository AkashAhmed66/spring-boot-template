package com.template.springboot.common.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * CORS allowlist driven entirely from configuration.
 *
 * <p>Origins go in {@link #allowedOriginPatterns} (supports {@code *} only as a port wildcard,
 * e.g. {@code http://localhost:*}). Never set this to a bare {@code *} when {@link #allowCredentials}
 * is true — that lets any site on the internet make credentialed requests against your API.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.security.cors")
public class CorsProperties {

    /** Explicit list of allowed origin patterns. Use ports/wildcards, NOT a bare {@code *}. */
    private List<String> allowedOriginPatterns = List.of(
            "http://localhost:*",
            "http://127.0.0.1:*"
    );

    private List<String> allowedMethods = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");

    private List<String> allowedHeaders = List.of("*");

    private List<String> exposedHeaders = List.of(
            "Authorization", "X-Request-Id", "X-RateLimit-Limit", "X-RateLimit-Remaining", "Retry-After");

    private boolean allowCredentials = true;

    /** Browser cache lifetime for preflight responses, in seconds. */
    private long maxAgeSeconds = 3600;
}
