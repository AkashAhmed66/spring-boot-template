package com.template.springboot.modules.audit.filter;

import tools.jackson.databind.ObjectMapper;
import com.template.springboot.common.security.SecurityUtils;
import com.template.springboot.modules.audit.config.AuditProperties;
import com.template.springboot.modules.audit.context.AuditContext;
import com.template.springboot.modules.audit.context.AuditContextHolder;
import com.template.springboot.modules.audit.entity.AuditLog;
import com.template.springboot.modules.audit.service.AuditLogService;
import com.template.springboot.modules.audit.util.AuditBodyMasker;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.regex.Pattern;

/**
 * Captures every API request handled by this service and persists an audit row asynchronously.
 * Runs after {@code RequestLoggingFilter} so the request-id is already in MDC and on the response.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "app.audit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AuditLogFilter extends OncePerRequestFilter {

    private final AuditLogService auditLogService;
    private final AuditProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!properties.isEnabled()) return true;

        String uri = request.getRequestURI();

        // Skip non-API traffic when include-prefixes are configured.
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

        int cacheLimit = Math.max(properties.getMaxBodyLength(), 1024);
        ContentCachingRequestWrapper req = new ContentCachingRequestWrapper(request, cacheLimit);
        ContentCachingResponseWrapper res = new ContentCachingResponseWrapper(response);

        long start = System.currentTimeMillis();
        String errorMessage = null;
        try {
            chain.doFilter(req, res);
        } catch (Exception ex) {
            errorMessage = truncate(ex.getClass().getSimpleName() + ": " + ex.getMessage(), 1000);
            throw ex;
        } finally {
            try {
                AuditContext ctx = AuditContextHolder.get();
                if (ctx == null || !ctx.isSkip()) {
                    AuditLog entry = build(req, res, ctx, System.currentTimeMillis() - start, errorMessage);
                    auditLogService.save(entry);
                }
            } catch (Exception ex) {
                log.warn("Audit capture failed for {} {}: {}",
                        request.getMethod(), request.getRequestURI(), ex.getMessage());
            } finally {
                AuditContextHolder.clear();
                // Must copy the cached body back to the real response or the client gets empty payload.
                res.copyBodyToResponse();
            }
        }
    }

    private AuditLog build(ContentCachingRequestWrapper req,
                           ContentCachingResponseWrapper res,
                           AuditContext ctx,
                           long durationMs,
                           String errorMessage) {

        String reqBody = properties.isCaptureRequestBody()
                ? readBody(req.getContentAsByteArray(), req.getCharacterEncoding())
                : null;
        String resBody = properties.isCaptureResponseBody()
                ? readBody(res.getContentAsByteArray(), res.getCharacterEncoding())
                : null;

        reqBody = AuditBodyMasker.truncate(
                AuditBodyMasker.mask(reqBody, objectMapper, properties.getMaskedFields()),
                properties.getMaxBodyLength());
        resBody = AuditBodyMasker.truncate(
                AuditBodyMasker.mask(resBody, objectMapper, properties.getMaskedFields()),
                properties.getMaxBodyLength());

        return AuditLog.builder()
                .requestId(req.getHeader("X-Request-Id"))
                .timestamp(Instant.now())
                .durationMs(durationMs)
                .userId(SecurityUtils.getCurrentUserId().orElse(null))
                .username(SecurityUtils.getCurrentUsername().orElse(null))
                .method(req.getMethod())
                .path(req.getRequestURI())
                .queryString(truncate(req.getQueryString(), 2000))
                .statusCode(res.getStatus())
                .action(ctx == null ? null : ctx.getAction())
                .resourceType(ctx == null ? null : ctx.getResourceType())
                .resourceId(ctx == null ? null : ctx.getResourceId())
                .clientIp(resolveClientIp(req))
                .userAgent(truncate(req.getHeader("User-Agent"), 500))
                .requestBody(reqBody)
                .responseBody(resBody)
                .errorMessage(errorMessage)
                .build();
    }

    private static String readBody(byte[] bytes, String encoding) {
        if (bytes == null || bytes.length == 0) return null;
        try {
            return new String(bytes, encoding == null ? StandardCharsets.UTF_8.name() : encoding);
        } catch (Exception ex) {
            return "[unreadable: " + ex.getMessage() + "]";
        }
    }

    private static String resolveClientIp(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return req.getRemoteAddr();
    }

    private static String truncate(String s, int max) {
        if (s == null || s.length() <= max) return s;
        return s.substring(0, max);
    }
}
