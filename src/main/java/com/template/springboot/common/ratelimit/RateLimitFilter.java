package com.template.springboot.common.ratelimit;

import com.template.springboot.common.security.SecurityUtils;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Global API rate limiter. Wired into the security chain after JwtAuthenticationFilter so it can
 * key the bucket by the authenticated username when present, and fall back to client IP otherwise.
 *
 * <p>On a 429 response the filter sets {@code Retry-After} (seconds), {@code X-RateLimit-Limit},
 * {@code X-RateLimit-Remaining}, and writes a JSON body matching the project's {@code ApiResponse}.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.rate-limit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitProperties properties;
    private final RateLimitService rateLimitService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.isEnabled()) return true;
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;

        String uri = request.getRequestURI();
        if (!properties.getIncludePathPrefixes().isEmpty()
                && properties.getIncludePathPrefixes().stream().noneMatch(uri::startsWith)) {
            return true;
        }
        for (String pattern : properties.getExcludePathPatterns()) {
            if (Pattern.compile(pattern).matcher(uri).matches()) return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String identity = resolveIdentity(request);
        boolean isAuthPath = properties.getAuthPathPrefixes().stream()
                .anyMatch(p -> request.getRequestURI().startsWith(p));

        Bucket bucket = isAuthPath
                ? rateLimitService.resolveAuth(identity)
                : rateLimitService.resolveDefault(identity);

        long limit = isAuthPath ? properties.getAuthCapacity() : properties.getCapacity();
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, probe.getRemainingTokens())));

        if (probe.isConsumed()) {
            chain.doFilter(request, response);
            return;
        }

        long retryAfterSeconds = Math.max(1, probe.getNanosToWaitForRefill() / 1_000_000_000L);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"success\":false,\"message\":\"Rate limit exceeded. Retry in " + retryAfterSeconds + "s.\"," +
                "\"errors\":null,\"timestamp\":\"" + java.time.Instant.now() + "\"}");
    }

    private static String resolveIdentity(HttpServletRequest request) {
        return SecurityUtils.getCurrentUsername()
                .map(u -> "user:" + u)
                .orElseGet(() -> "ip:" + resolveClientIp(request));
    }

    private static String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return request.getRemoteAddr();
    }
}
