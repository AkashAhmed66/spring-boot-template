package com.template.springboot.modules.audit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_logs_timestamp",   columnList = "timestamp"),
        @Index(name = "idx_audit_logs_user_id",     columnList = "user_id"),
        @Index(name = "idx_audit_logs_username",    columnList = "username"),
        @Index(name = "idx_audit_logs_path",        columnList = "path"),
        @Index(name = "idx_audit_logs_action",      columnList = "action"),
        @Index(name = "idx_audit_logs_resource",    columnList = "resource_type,resource_id"),
        @Index(name = "idx_audit_logs_request_id",  columnList = "request_id"),
        @Index(name = "idx_audit_logs_status_code", columnList = "status_code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", length = 64)
    private String requestId;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", length = 100)
    private String username;

    @Column(name = "method", nullable = false, length = 10)
    private String method;

    @Column(name = "path", nullable = false, length = 500)
    private String path;

    @Column(name = "query_string", length = 2000)
    private String queryString;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "action", length = 100)
    private String action;

    @Column(name = "resource_type", length = 100)
    private String resourceType;

    @Column(name = "resource_id", length = 100)
    private String resourceId;

    @Column(name = "client_ip", length = 64)
    private String clientIp;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "request_body", columnDefinition = "TEXT")
    private String requestBody;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;
}
