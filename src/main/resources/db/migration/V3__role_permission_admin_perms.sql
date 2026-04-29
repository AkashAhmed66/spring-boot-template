-- New permissions for managing roles & permissions themselves
INSERT INTO permissions (name, description, created_at, updated_at, created_by, updated_by) VALUES
    ('ROLE_DELETE',      'Delete roles',       NOW(6), NOW(6), 'system', 'system'),
    ('PERMISSION_READ',  'Read permissions',   NOW(6), NOW(6), 'system', 'system'),
    ('PERMISSION_WRITE', 'Manage permissions', NOW(6), NOW(6), 'system', 'system');

-- Grant the new permissions to ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ADMIN'
  AND p.name IN ('ROLE_DELETE', 'PERMISSION_READ', 'PERMISSION_WRITE');
