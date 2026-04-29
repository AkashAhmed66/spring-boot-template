package com.template.springboot.common.security;

import java.io.Serializable;

public record AuthenticatedUser(Long id, String username) implements Serializable {
    @Override
    public String toString() {
        return username;
    }
}
