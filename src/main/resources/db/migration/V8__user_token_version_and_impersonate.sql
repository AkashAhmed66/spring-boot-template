-- Token versioning: every issued JWT carries the user's current token_version. Bumping
-- this counter invalidates every outstanding access/refresh token for that user
-- (force-logout, password reset, role change, deactivation, etc.) without needing a blacklist.
ALTER TABLE users
    ADD COLUMN token_version BIGINT NOT NULL DEFAULT 0;

-- Permission gating the admin impersonation endpoint.
INSERT INTO permissions (name, description, created_at, updated_at, created_by, updated_by) VALUES
    ('USER_IMPERSONATE', 'Sign in as another user (admin-only)', NOW(6), NOW(6), 'system', 'system');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ADMIN' AND p.name = 'USER_IMPERSONATE';
