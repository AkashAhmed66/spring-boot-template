package com.template.springboot.common.ratelimit;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * Configuration for the global API rate limiter.
 *
 * <p>One bucket per identity (authenticated username, or client IP for anonymous calls).
 * Bucket4j tokens refill at {@code refillTokens} per {@code refillPeriod}, capped at {@code capacity}.
 *
 * <p>Auth endpoints get a separate, stricter bucket so that login flooding cannot exhaust the
 * regular per-IP allowance.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    /** Master switch — when false, the filter is not even registered. */
    private boolean enabled = true;

    /** Default bucket: capacity (max burst tokens). */
    private int capacity = 60;

    /** Default bucket: tokens to add each refill period. Often equals {@link #capacity} for "N per period". */
    private int refillTokens = 60;

    /** Default bucket: how often {@code refillTokens} are added. */
    private Duration refillPeriod = Duration.ofMinutes(1);

    /** Stricter bucket applied to {@link #authPathPrefixes}. */
    private int authCapacity = 10;
    private int authRefillTokens = 10;
    private Duration authRefillPeriod = Duration.ofMinutes(1);

    /** Path prefixes that get the stricter auth bucket. */
    private List<String> authPathPrefixes = List.of("/api/v1/auth/");

    /** Only paths starting with one of these are rate-limited at all. */
    private List<String> includePathPrefixes = List.of("/api/");

    /** Regex (full-match) patterns to skip entirely. */
    private List<String> excludePathPatterns = List.of(
            "/actuator/.*",
            ".*/swagger-ui.*",
            "/v3/api-docs.*"
    );
}
