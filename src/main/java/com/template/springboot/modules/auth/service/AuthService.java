package com.template.springboot.modules.auth.service;

import com.template.springboot.modules.auth.dto.AuthResponse;
import com.template.springboot.modules.auth.dto.ForgotPasswordRequest;
import com.template.springboot.modules.auth.dto.LoginRequest;
import com.template.springboot.modules.auth.dto.RefreshRequest;
import com.template.springboot.modules.auth.dto.RegisterRequest;
import com.template.springboot.modules.auth.dto.ResetPasswordRequest;
import com.template.springboot.modules.session.dto.DeviceInfo;
import com.template.springboot.modules.session.dto.SessionResponse;

import java.util.List;

public interface AuthService {

    AuthResponse register(RegisterRequest request, DeviceInfo device);

    AuthResponse login(LoginRequest request, DeviceInfo device);

    AuthResponse refresh(RefreshRequest request, DeviceInfo device);

    void logout();

    int logoutAll();

    void revokeSession(Long sessionId);

    List<SessionResponse> listMySessions();

    AuthResponse impersonate(Long targetUserId, DeviceInfo device);

    /** Always succeeds — never reveals whether the email exists in the system. */
    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);
}
