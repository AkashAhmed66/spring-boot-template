package com.template.springboot.modules.session.dto;

import java.io.Serializable;
import java.time.Instant;

public record SessionContext(Long sessionId, Long userId, Instant expiresAt, Instant revokedAt)
        implements Serializable {

    public boolean isActive() {
        return revokedAt == null && expiresAt.isAfter(Instant.now());
    }
}
