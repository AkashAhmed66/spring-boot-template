package com.template.springboot.modules.session.service;

import com.template.springboot.modules.session.dto.SessionContext;
import com.template.springboot.modules.session.dto.SessionResponse;
import com.template.springboot.modules.session.entity.UserSession;

import java.time.Instant;
import java.util.List;

public interface UserSessionService {

    UserSession create(Long userId, Long impersonatorId, String userAgent, String ipAddress,
                       String deviceName, Instant expiresAt);

    SessionContext findContext(Long sessionId);

    void touch(Long sessionId);

    void revoke(Long sessionId, String reason);

    int revokeAllForUser(Long userId, String reason);

    List<SessionResponse> listForUser(Long userId, Long currentSessionId);

    List<SessionResponse> listAllActive(Long currentSessionId);

    UserSession requireOwnedBy(Long sessionId, Long userId);
}
