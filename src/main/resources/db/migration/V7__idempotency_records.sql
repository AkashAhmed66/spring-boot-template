-- Idempotency: dedupes non-idempotent requests so retries do not double-execute.
-- A row is INSERTed at the start of a protected request (status=IN_PROGRESS) and
-- UPDATEd with the serialized response on completion. Retries within the TTL window
-- replay the stored response without re-running the handler.

CREATE TABLE idempotency_records (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    idempotency_key VARCHAR(128) NOT NULL,
    user_key        VARCHAR(128) NOT NULL,
    method          VARCHAR(10)  NOT NULL,
    path            VARCHAR(500) NOT NULL,
    request_hash    VARCHAR(64)  NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    status_code     INT          NULL,
    response_body   TEXT         NULL,
    created_at      DATETIME(6)  NOT NULL,
    completed_at    DATETIME(6)  NULL,
    expires_at      DATETIME(6)  NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_idempotency_user_key (user_key, idempotency_key),
    KEY idx_idempotency_expires_at (expires_at)
);
