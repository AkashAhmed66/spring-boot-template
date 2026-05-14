-- Generic admin-override permissions. A user holding any of these bypasses
-- per-record ownership checks (e.g. "is this session mine?") in the service layer.
-- They are coarse on purpose — fine-grained gates (USER_READ, SESSION_REVOKE, etc.)
-- still apply at the endpoint level for non-admin roles.
INSERT INTO permissions (name, description, created_at, updated_at, created_by, updated_by) VALUES
    ('ADMIN_READ',   'Read any user-owned resource',   NOW(6), NOW(6), 'system', 'system'),
    ('ADMIN_WRITE',  'Create resources for any user',  NOW(6), NOW(6), 'system', 'system'),
    ('ADMIN_EDIT',   'Edit any user-owned resource',   NOW(6), NOW(6), 'system', 'system'),
    ('ADMIN_DELETE', 'Delete any user-owned resource', NOW(6), NOW(6), 'system', 'system');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ADMIN'
  AND p.name IN ('ADMIN_READ', 'ADMIN_WRITE', 'ADMIN_EDIT', 'ADMIN_DELETE');
