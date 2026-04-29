package com.template.springboot.modules.auth.controller;

import com.template.springboot.common.dto.ApiResponse;
import com.template.springboot.modules.auth.dto.LoginRequest;
import com.template.springboot.modules.auth.dto.RefreshRequest;
import com.template.springboot.modules.auth.dto.RegisterRequest;
import com.template.springboot.modules.auth.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ApiResponse register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.created(authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse login(@Valid @RequestBody LoginRequest request) {
        return new ApiResponse(authService.login(request), "Login successful");
    }

    @PostMapping("/refresh")
    public ApiResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return new ApiResponse(authService.refresh(request), "Token refreshed");
    }
}
