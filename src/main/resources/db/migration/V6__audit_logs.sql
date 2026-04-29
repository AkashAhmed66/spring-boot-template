-- Audit log: one row per HTTP request handled by the API.
-- Captures who did what, against which resource, with which payload, and how the system responded.
-- Body fields are TEXT (not JSON) to keep MySQL/Postgres compatibility and tolerate non-JSON payloads.

CREATE TABLE audit_logs (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    request_id      VARCHAR(64)  NULL,
    timestamp       DATETIME(6)  NOT NULL,
    duration_ms     BIGINT       NULL,

    user_id         BIGINT       NULL,
    username        VARCHAR(100) NULL,

    method          VARCHAR(10)  NOT NULL,
    path            VARCHAR(500) NOT NULL,
    query_string    VARCHAR(2000) NULL,
    status_code     INT          NULL,

    action          VARCHAR(100) NULL,
    resource_type   VARCHAR(100) NULL,
    resource_id     VARCHAR(100) NULL,

    client_ip       VARCHAR(64)  NULL,
    user_agent      VARCHAR(500) NULL,

    request_body    TEXT         NULL,
    response_body   TEXT         NULL,
    error_message   VARCHAR(1000) NULL,

    PRIMARY KEY (id),
    KEY idx_audit_logs_timestamp     (timestamp),
    KEY idx_audit_logs_user_id       (user_id),
    KEY idx_audit_logs_username      (username),
    KEY idx_audit_logs_path          (path),
    KEY idx_audit_logs_action        (action),
    KEY idx_audit_logs_resource      (resource_type, resource_id),
    KEY idx_audit_logs_request_id    (request_id),
    KEY idx_audit_logs_status_code   (status_code)
);

-- Permissions for viewing audit logs (write happens internally, never via API).
INSERT INTO permissions (name, description, created_at, updated_at, created_by, updated_by) VALUES
    ('AUDIT_READ', 'Read audit log entries', NOW(6), NOW(6), 'system', 'system');

-- Grant to ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ADMIN' AND p.name = 'AUDIT_READ';
