package com.template.springboot.modules.auth.service;

import com.template.springboot.modules.auth.dto.AuthResponse;
import com.template.springboot.modules.auth.dto.LoginRequest;
import com.template.springboot.modules.auth.dto.RefreshRequest;
import com.template.springboot.modules.auth.dto.RegisterRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshRequest request);
}
