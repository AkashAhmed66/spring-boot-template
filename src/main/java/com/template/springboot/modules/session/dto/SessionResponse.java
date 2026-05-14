package com.template.springboot.modules.session.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SessionResponse {

    private Long id;
    private String deviceName;
    private String userAgent;
    private String ipAddress;
    private Long impersonatorId;
    private Instant issuedAt;
    private Instant lastUsedAt;
    private Instant expiresAt;
    private boolean current;
}
