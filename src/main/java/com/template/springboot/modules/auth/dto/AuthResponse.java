package com.template.springboot.modules.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String tokenType;
    private String accessToken;
    private String refreshToken;
    private long expiresIn;
    private Long userId;
    private String username;
    private String email;
    private Set<String> authorities;
}
