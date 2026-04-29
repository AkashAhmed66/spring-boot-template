package com.template.springboot.modules.auth.dto;

import java.util.Set;

public record AuthResponse(
        String tokenType,
        String accessToken,
        String refreshToken,
        long expiresIn,
        Long userId,
        String username,
        String email,
        Set<String> authorities) {
}
