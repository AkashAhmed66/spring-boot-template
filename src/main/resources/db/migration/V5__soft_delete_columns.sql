-- Soft-delete columns on every audited table.
-- A row is "live" when deleted_at IS NULL; otherwise it has been soft-deleted by `deleted_by` at `deleted_at`.

ALTER TABLE permissions
    ADD COLUMN deleted_at DATETIME(6) NULL,
    ADD COLUMN deleted_by VARCHAR(100) NULL;

ALTER TABLE roles
    ADD COLUMN deleted_at DATETIME(6) NULL,
    ADD COLUMN deleted_by VARCHAR(100) NULL;

ALTER TABLE users
    ADD COLUMN deleted_at DATETIME(6) NULL,
    ADD COLUMN deleted_by VARCHAR(100) NULL;

ALTER TABLE products
    ADD COLUMN deleted_at DATETIME(6) NULL,
    ADD COLUMN deleted_by VARCHAR(100) NULL;

-- Indexes — every read query is filtered with `deleted_at IS NULL`,
-- so an index on this column dramatically helps planner choose the right path.
CREATE INDEX idx_permissions_deleted_at ON permissions (deleted_at);
CREATE INDEX idx_roles_deleted_at       ON roles       (deleted_at);
CREATE INDEX idx_users_deleted_at       ON users       (deleted_at);
CREATE INDEX idx_products_deleted_at    ON products    (deleted_at);
