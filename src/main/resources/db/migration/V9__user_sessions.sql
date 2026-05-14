-- Per-device session tracking replaces the user-level token_version added in V8.
-- Every successful login (or impersonation) inserts one row; every issued JWT carries
-- that session's id (`sid` claim). The auth filter validates the session row on each
-- request, so revoking one row logs out exactly one device without affecting others.
-- "Logout everywhere" = revoke every row for the user.
ALTER TABLE users DROP COLUMN token_version;

CREATE TABLE user_sessions (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    user_id          BIGINT       NOT NULL,
    impersonator_id  BIGINT       NULL,
    device_name      VARCHAR(150) NULL,
    user_agent       VARCHAR(500) NULL,
    ip_address       VARCHAR(64)  NULL,
    issued_at        DATETIME(6)  NOT NULL,
    last_used_at     DATETIME(6)  NOT NULL,
    expires_at       DATETIME(6)  NOT NULL,
    revoked_at       DATETIME(6)  NULL,
    revoked_reason   VARCHAR(100) NULL,
    PRIMARY KEY (id),
    KEY idx_user_sessions_user_active (user_id, revoked_at),
    KEY idx_user_sessions_expires_at (expires_at),
    CONSTRAINT fk_user_sessions_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
