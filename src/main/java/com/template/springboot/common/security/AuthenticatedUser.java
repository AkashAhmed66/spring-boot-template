package com.template.springboot.common.security;

import java.io.Serializable;

public record AuthenticatedUser(Long id, String username, Long sessionId, Long impersonatedBy)
        implements Serializable {

    public AuthenticatedUser(Long id, String username) {
        this(id, username, null, null);
    }

    public boolean isImpersonated() {
        return impersonatedBy != null;
    }

    @Override
    public String toString() {
        return username;
    }
}
