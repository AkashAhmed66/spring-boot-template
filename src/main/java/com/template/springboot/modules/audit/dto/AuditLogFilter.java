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
public class AuditLogFilter {

    private String username;
    private Long userId;
    private String method;
    private String path;
    private String action;
    private String resourceType;
    private String resourceId;
    private Integer statusCode;
    private String requestId;
    private Instant from;
    private Instant to;
}
