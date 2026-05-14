package com.template.springboot.modules.auth.controller;

import com.template.springboot.common.dto.ApiResponse;
import com.template.springboot.common.security.HasPermission;
import com.template.springboot.modules.audit.annotation.Auditable;
import com.template.springboot.modules.auth.dto.ForgotPasswordRequest;
import com.template.springboot.modules.auth.dto.LoginRequest;
import com.template.springboot.modules.auth.dto.RefreshRequest;
import com.template.springboot.modules.auth.dto.RegisterRequest;
import com.template.springboot.modules.auth.dto.ResetPasswordRequest;
import com.template.springboot.modules.auth.service.AuthService;
import com.template.springboot.modules.permission.enums.PermissionName;
import com.template.springboot.modules.session.dto.DeviceInfo;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    @Auditable(action = "AUTH_REGISTER", resourceType = "User", resourceId = "#request.username")
    public ApiResponse register(@Valid @RequestBody RegisterRequest request, HttpServletRequest http) {
        return ApiResponse.created(authService.register(request, deviceFrom(http)));
    }

    @PostMapping("/login")
    @Auditable(action = "AUTH_LOGIN", resourceType = "User", resourceId = "#request.usernameOrEmail")
    public ApiResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return new ApiResponse(authService.login(request, deviceFrom(http)), "Login successful");
    }

    @PostMapping("/refresh")
    @Auditable(action = "AUTH_REFRESH", resourceType = "User")
    public ApiResponse refresh(@Valid @RequestBody RefreshRequest request, HttpServletRequest http) {
        return new ApiResponse(authService.refresh(request, deviceFrom(http)), "Token refreshed");
    }

    @PostMapping("/forgot-password")
    @Auditable(action = "AUTH_FORGOT_PASSWORD", resourceType = "User", resourceId = "#request.email")
    public ApiResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        // Always return the same message — never reveal whether the email exists.
        return ApiResponse.message("If an account with that email exists, a reset link has been sent");
    }

    @PostMapping("/reset-password")
    @Auditable(action = "AUTH_RESET_PASSWORD", resourceType = "User")
    public ApiResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.message("Password updated — please sign in again");
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    @Auditable(action = "AUTH_LOGOUT", resourceType = "Session")
    public ApiResponse logout() {
        authService.logout();
        return ApiResponse.message("Logged out from this device");
    }

    @PostMapping("/logout-all")
    @SecurityRequirement(name = "bearerAuth")
    @Auditable(action = "AUTH_LOGOUT_ALL", resourceType = "User")
    public ApiResponse logoutAll() {
        int count = authService.logoutAll();
        return ApiResponse.message("Logged out from " + count + " device(s)");
    }

    @GetMapping("/sessions")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse mySessions() {
        return new ApiResponse(authService.listMySessions());
    }

    @DeleteMapping("/sessions/{sessionId}")
    @SecurityRequirement(name = "bearerAuth")
    @Auditable(action = "AUTH_REVOKE_SESSION", resourceType = "Session", resourceId = "#sessionId")
    public ApiResponse revokeSession(@PathVariable Long sessionId) {
        authService.revokeSession(sessionId);
        return ApiResponse.message("Session revoked");
    }

    @PostMapping("/impersonate/{userId}")
    @SecurityRequirement(name = "bearerAuth")
    @HasPermission(PermissionName.USER_IMPERSONATE)
    @Auditable(action = "AUTH_IMPERSONATE", resourceType = "User", resourceId = "#userId")
    public ApiResponse impersonate(@PathVariable Long userId, HttpServletRequest http) {
        return new ApiResponse(authService.impersonate(userId, deviceFrom(http)), "Impersonation tokens issued");
    }

    private static DeviceInfo deviceFrom(HttpServletRequest request) {
        String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
        return new DeviceInfo(deviceLabel(userAgent), userAgent, clientIp(request));
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Pull a short "browser on OS" label out of the User-Agent header — good enough to make
     * the session list readable ("Chrome on Windows"), nothing more. Returns null when the
     * UA is missing or unrecognisable; the raw header is still stored separately on the
     * session row, so no information is lost.
     */
    static String deviceLabel(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) return null;
        String ua = userAgent;
        String os = null;
        if      (ua.contains("Windows NT"))                       os = "Windows";
        else if (ua.contains("Mac OS X") || ua.contains("Macintosh")) os = "macOS";
        else if (ua.contains("Android"))                          os = "Android";
        else if (ua.contains("iPhone") || ua.contains("iPad") || ua.contains("iOS")) os = "iOS";
        else if (ua.contains("CrOS"))                             os = "ChromeOS";
        else if (ua.contains("Linux"))                            os = "Linux";

        String browser = null;
        if      (ua.contains("Edg/"))                             browser = "Edge";
        else if (ua.contains("OPR/") || ua.contains("Opera"))     browser = "Opera";
        else if (ua.contains("Firefox/"))                         browser = "Firefox";
        else if (ua.contains("Chrome/") && !ua.contains("Chromium")) browser = "Chrome";
        else if (ua.contains("Safari/"))                          browser = "Safari";
        else if (ua.contains("curl/"))                            browser = "curl";
        else if (ua.contains("PostmanRuntime"))                   browser = "Postman";
        else if (ua.contains("insomnia"))                         browser = "Insomnia";

        if (browser != null && os != null) return browser + " on " + os;
        if (browser != null)               return browser;
        if (os != null)                    return os;
        return null;   // raw UA still kept on the session row
    }
}
