package com.template.springboot.modules.audit.dto;

import com.template.springboot.modules.audit.entity.AuditLog;

import java.time.Instant;

public record AuditLogResponse(
        Long id,
        String requestId,
        Instant timestamp,
        Long durationMs,
        Long userId,
        String username,
        String method,
        String path,
        String queryString,
        Integer statusCode,
        String action,
        String resourceType,
        String resourceId,
        String clientIp,
        String userAgent,
        String requestBody,
        String responseBody,
        String errorMessage) {

    public static AuditLogResponse from(AuditLog a) {
        return new AuditLogResponse(
                a.getId(),
                a.getRequestId(),
                a.getTimestamp(),
                a.getDurationMs(),
                a.getUserId(),
                a.getUsername(),
                a.getMethod(),
                a.getPath(),
                a.getQueryString(),
                a.getStatusCode(),
                a.getAction(),
                a.getResourceType(),
                a.getResourceId(),
                a.getClientIp(),
                a.getUserAgent(),
                a.getRequestBody(),
                a.getResponseBody(),
                a.getErrorMessage());
    }
}
