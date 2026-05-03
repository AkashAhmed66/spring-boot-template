package com.template.springboot.modules.audit.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {

    private Long id;
    private String requestId;
    private Instant timestamp;
    private Long durationMs;
    private Long userId;
    private String username;
    private String method;
    private String path;
    private String queryString;
    private Integer statusCode;
    private String action;
    private String resourceType;
    private String resourceId;
    private String clientIp;
    private String userAgent;
    private String requestBody;
    private String responseBody;
    private String errorMessage;
}
