package com.template.springboot.modules.session.serviceImpl;

import com.template.springboot.common.exception.ResourceNotFoundException;
import com.template.springboot.common.security.SecurityUtils;
import com.template.springboot.modules.permission.enums.PermissionName;
import com.template.springboot.modules.session.dto.SessionContext;
import com.template.springboot.modules.session.dto.SessionResponse;
import com.template.springboot.modules.session.entity.UserSession;
import com.template.springboot.modules.session.repository.UserSessionRepository;
import com.template.springboot.modules.session.service.UserSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserSessionServiceImpl implements UserSessionService {

    public static final String SESSIONS_CACHE = "userSessions";

    private final UserSessionRepository repository;

    @Override
    @Transactional
    public UserSession create(Long userId, Long impersonatorId, String userAgent, String ipAddress,
                              String deviceName, Instant expiresAt) {
        Instant now = Instant.now();
        UserSession session = new UserSession();
        session.setUserId(userId);
        session.setImpersonatorId(impersonatorId);
        session.setUserAgent(truncate(userAgent, 500));
        session.setIpAddress(truncate(ipAddress, 64));
        session.setDeviceName(truncate(deviceName, 150));
        session.setIssuedAt(now);
        session.setLastUsedAt(now);
        session.setExpiresAt(expiresAt);
        return repository.save(session);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = SESSIONS_CACHE, key = "#sessionId", unless = "#result == null")
    public SessionContext findContext(Long sessionId) {
        return repository.findById(sessionId)
                .map(s -> new SessionContext(s.getId(), s.getUserId(), s.getExpiresAt(), s.getRevokedAt()))
                .orElse(null);
    }

    @Override
    @Transactional
    public void touch(Long sessionId) {
        repository.findById(sessionId).ifPresent(s -> s.setLastUsedAt(Instant.now()));
    }

    @Override
    @Transactional
    @CacheEvict(value = SESSIONS_CACHE, key = "#sessionId")
    public void revoke(Long sessionId, String reason) {
        repository.findById(sessionId).ifPresent(s -> {
            if (s.getRevokedAt() == null) {
                s.setRevokedAt(Instant.now());
                s.setRevokedReason(truncate(reason, 100));
            }
        });
    }

    @Override
    @Transactional
    @CacheEvict(value = SESSIONS_CACHE, allEntries = true)
    public int revokeAllForUser(Long userId, String reason) {
        return repository.revokeAllByUserId(userId, Instant.now(), truncate(reason, 100));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionResponse> listForUser(Long userId, Long currentSessionId) {
        return repository.findActiveByUserId(userId).stream()
                .map(s -> toResponse(s, currentSessionId))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionResponse> listAllActive(Long currentSessionId) {
        return repository.findAllActive().stream()
                .map(s -> toResponse(s, currentSessionId))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserSession requireOwnedBy(Long sessionId, Long userId) {
        UserSession session = repository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session", sessionId));
        if (SecurityUtils.hasAnyAuthority(PermissionName.ADMIN_ANY)) {
            return session;
        }
        if (!Objects.equals(session.getUserId(), userId)) {
            throw new ResourceNotFoundException("Session", sessionId);
        }
        return session;
    }

    private static SessionResponse toResponse(UserSession s, Long currentSessionId) {
        return SessionResponse.builder()
                .id(s.getId())
                .deviceName(s.getDeviceName())
                .userAgent(s.getUserAgent())
                .ipAddress(s.getIpAddress())
                .impersonatorId(s.getImpersonatorId())
                .issuedAt(s.getIssuedAt())
                .lastUsedAt(s.getLastUsedAt())
                .expiresAt(s.getExpiresAt())
                .current(Objects.equals(s.getId(), currentSessionId))
                .build();
    }

    private static String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}
