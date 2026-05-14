package com.template.springboot.modules.auth.serviceImpl;

import com.template.springboot.common.exception.BadRequestException;
import com.template.springboot.common.exception.DuplicateResourceException;
import com.template.springboot.common.exception.ResourceNotFoundException;
import com.template.springboot.common.mail.EmailProperties;
import com.template.springboot.common.mail.EmailService;
import com.template.springboot.common.security.CustomUserDetails;
import com.template.springboot.common.security.JwtService;
import com.template.springboot.common.security.SecurityUtils;
import com.template.springboot.modules.auth.dto.AuthResponse;
import com.template.springboot.modules.auth.dto.ForgotPasswordRequest;
import com.template.springboot.modules.auth.dto.LoginRequest;
import com.template.springboot.modules.auth.dto.RefreshRequest;
import com.template.springboot.modules.auth.dto.RegisterRequest;
import com.template.springboot.modules.auth.dto.ResetPasswordRequest;
import com.template.springboot.modules.auth.entity.PasswordResetToken;
import com.template.springboot.modules.auth.repository.PasswordResetTokenRepository;
import com.template.springboot.modules.auth.service.AuthService;
import com.template.springboot.modules.permission.enums.PermissionName;
import com.template.springboot.modules.role.entity.Role;
import com.template.springboot.modules.role.enums.RoleName;
import com.template.springboot.modules.role.repository.RoleRepository;
import com.template.springboot.modules.session.dto.DeviceInfo;
import com.template.springboot.modules.session.dto.SessionResponse;
import com.template.springboot.modules.session.entity.UserSession;
import com.template.springboot.modules.session.service.UserSessionService;
import com.template.springboot.modules.user.entity.User;
import com.template.springboot.modules.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserSessionService sessionService;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final EmailService emailService;
    private final EmailProperties emailProperties;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request, DeviceInfo device) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email is already in use");
        }
        Role defaultRole = roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> new ResourceNotFoundException("Default role missing: " + RoleName.USER));

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEnabled(true);
        user.getRoles().add(defaultRole);
        userRepository.save(user);

        return issueTokens(new CustomUserDetails(user), device, null);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, DeviceInfo device) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsernameOrEmail(), request.getPassword()));
        CustomUserDetails principal = (CustomUserDetails) auth.getPrincipal();
        if (!principal.isEnabled()) {
            throw new BadRequestException("Account is deactivated");
        }
        return issueTokens(principal, device, null);
    }

    @Override
    @Transactional
    public AuthResponse refresh(RefreshRequest request, DeviceInfo device) {
        Claims claims;
        try {
            claims = jwtService.parse(request.getRefreshToken());
        } catch (JwtException ex) {
            throw new BadRequestException("Invalid refresh token");
        }
        if (!jwtService.isRefreshToken(claims)) {
            throw new BadRequestException("Token is not a refresh token");
        }
        Long uid = jwtService.extractUserId(claims);
        Long sessionId = jwtService.extractSessionId(claims);
        if (uid == null || sessionId == null) {
            throw new BadRequestException("Invalid refresh token");
        }

        var session = sessionService.findContext(sessionId);
        if (session == null || !Objects.equals(session.userId(), uid) || !session.isActive()) {
            throw new BadRequestException("Refresh token has been revoked or expired");
        }

        User user = userRepository.findWithRolesById(uid)
                .orElseThrow(() -> new ResourceNotFoundException("User", claims.getSubject()));
        if (!user.isEnabled()) {
            sessionService.revoke(sessionId, "account-deactivated");
            throw new BadRequestException("Account is deactivated");
        }

        sessionService.touch(sessionId);
        Long impersonatedBy = jwtService.extractImpersonatedBy(claims);
        return buildResponse(new CustomUserDetails(user), sessionId, impersonatedBy);
    }

    @Override
    @Transactional
    public void logout() {
        Long sessionId = SecurityUtils.getCurrentSessionId()
                .orElseThrow(() -> new BadRequestException("Not authenticated"));
        sessionService.revoke(sessionId, "user-logout");
    }

    @Override
    @Transactional
    public int logoutAll() {
        Long userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new BadRequestException("Not authenticated"));
        return sessionService.revokeAllForUser(userId, "user-logout-all");
    }

    @Override
    @Transactional
    public void revokeSession(Long sessionId) {
        Long userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new BadRequestException("Not authenticated"));
        sessionService.requireOwnedBy(sessionId, userId);
        sessionService.revoke(sessionId, "user-revoked-device");
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionResponse> listMySessions() {
        Long userId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new BadRequestException("Not authenticated"));
        Long currentSessionId = SecurityUtils.getCurrentSessionId().orElse(null);
        if (SecurityUtils.hasAnyAuthority(PermissionName.ADMIN_ANY)) {
            return sessionService.listAllActive(currentSessionId);
        }
        return sessionService.listForUser(userId, currentSessionId);
    }

    @Override
    @Transactional
    public AuthResponse impersonate(Long targetUserId, DeviceInfo device) {
        Long adminId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new BadRequestException("Not authenticated"));
        if (adminId.equals(targetUserId)) {
            throw new BadRequestException("Cannot impersonate yourself");
        }
        User target = userRepository.findWithRolesById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", targetUserId));
        if (!target.isEnabled()) {
            throw new BadRequestException("Cannot impersonate a deactivated account");
        }
        return issueTokens(new CustomUserDetails(target), device, adminId);
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        // We don't reveal whether the email exists — same response either way. Sending
        // happens async so even response-time differences don't leak account existence.
        Optional<User> user = userRepository.findByEmail(request.getEmail());
        if (user.isEmpty() || !user.get().isEnabled()) {
            log.debug("forgotPassword: no active user for email={}", request.getEmail());
            return;
        }
        if (!emailService.isEnabled()) {
            log.warn("forgotPassword requested but email is disabled ({}) — token NOT issued for userId={}",
                    emailService.disabledReason(), user.get().getId());
            return;
        }

        String rawToken = generateRawToken();
        Instant now = Instant.now();
        PasswordResetToken token = new PasswordResetToken();
        token.setUserId(user.get().getId());
        token.setTokenHash(hash(rawToken));
        token.setCreatedAt(now);
        token.setExpiresAt(now.plus(emailProperties.getPasswordResetTtl()));
        resetTokenRepository.save(token);

        String resetUrl = emailProperties.getAppUrl() + "/reset-password?token=" + rawToken;
        Map<String, Object> model = new HashMap<>();
        model.put("username", user.get().getUsername());
        model.put("resetUrl", resetUrl);
        model.put("ttlMinutes", emailProperties.getPasswordResetTtl().toMinutes());
        emailService.sendTemplated("password-reset", user.get().getEmail(), "Reset your password", model);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = resetTokenRepository.findByTokenHash(hash(request.getToken()))
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));
        if (!token.isUsable()) {
            throw new BadRequestException("Invalid or expired reset token");
        }
        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", token.getUserId()));
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        token.setConsumedAt(Instant.now());
        // Invalidate any other outstanding reset tokens for this user.
        resetTokenRepository.invalidateAllForUser(user.getId(), Instant.now());
        // Kill every active session — password change should log out all devices.
        sessionService.revokeAllForUser(user.getId(), "password-reset");
    }

    private AuthResponse issueTokens(CustomUserDetails user, DeviceInfo device, Long impersonatedBy) {
        Instant expiresAt = Instant.now().plus(jwtService.getRefreshTokenTtl());
        UserSession session = sessionService.create(
                user.getId(), impersonatedBy,
                device.userAgent(), device.ipAddress(), device.deviceName(),
                expiresAt);
        return buildResponse(user, session.getId(), impersonatedBy);
    }

    private AuthResponse buildResponse(CustomUserDetails user, Long sessionId, Long impersonatedBy) {
        Set<String> authorities = user.authoritiesAsStrings();
        String access = jwtService.generateAccessToken(
                user.getUsername(), user.getId(), sessionId,
                authorities.stream().toList(), impersonatedBy);
        String refresh = jwtService.generateRefreshToken(
                user.getUsername(), user.getId(), sessionId, impersonatedBy);
        return new AuthResponse(
                "Bearer", access, refresh, jwtService.getAccessTokenTtlSeconds(),
                user.getId(), user.getUsername(), user.getEmail(), authorities,
                sessionId, impersonatedBy);
    }

    private static String generateRawToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Store only the hash so DB compromise alone cannot mint a working reset link. */
    private static String hash(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
