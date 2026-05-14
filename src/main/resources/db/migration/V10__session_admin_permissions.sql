-- Permissions gating the admin session-management endpoints (view + revoke any user's
-- session). Kept separate from USER_* because session inspection is more sensitive — it
-- exposes IP, user-agent, and device metadata that USER_READ does not.
INSERT INTO permissions (name, description, created_at, updated_at, created_by, updated_by) VALUES
    ('SESSION_READ',   'View any user session',   NOW(6), NOW(6), 'system', 'system'),
    ('SESSION_REVOKE', 'Revoke any user session', NOW(6), NOW(6), 'system', 'system');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ADMIN' AND p.name IN ('SESSION_READ', 'SESSION_REVOKE');
