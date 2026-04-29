package com.template.springboot.modules.audit.dto;

import java.time.Instant;

public record AuditLogFilter(
        String username,
        Long userId,
        String method,
        String path,
        String action,
        String resourceType,
        String resourceId,
        Integer statusCode,
        String requestId,
        Instant from,
        Instant to) {
}
