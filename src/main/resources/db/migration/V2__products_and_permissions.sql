-- Products
CREATE TABLE products (
    id          BIGINT         NOT NULL AUTO_INCREMENT,
    sku         VARCHAR(50)    NOT NULL,
    name        VARCHAR(200)   NOT NULL,
    description VARCHAR(1000)  NULL,
    price       DECIMAL(19, 2) NOT NULL,
    stock       INT            NOT NULL DEFAULT 0,
    status      VARCHAR(20)    NOT NULL DEFAULT 'DRAFT',
    created_at  DATETIME(6)    NOT NULL,
    updated_at  DATETIME(6)    NOT NULL,
    created_by  VARCHAR(100)   NULL,
    updated_by  VARCHAR(100)   NULL,
    version     BIGINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_products_sku (sku)
);

-- Product permissions
INSERT INTO permissions (name, description, created_at, updated_at, created_by, updated_by) VALUES
    ('PRODUCT_READ',   'Read products',   NOW(6), NOW(6), 'system', 'system'),
    ('PRODUCT_WRITE',  'Modify products', NOW(6), NOW(6), 'system', 'system'),
    ('PRODUCT_DELETE', 'Delete products', NOW(6), NOW(6), 'system', 'system');

-- Grant all product perms to ADMIN
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ADMIN' AND p.name IN ('PRODUCT_READ', 'PRODUCT_WRITE', 'PRODUCT_DELETE');

-- MANAGER gets read + write (not delete)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'MANAGER' AND p.name IN ('PRODUCT_READ', 'PRODUCT_WRITE');

-- USER gets read-only access
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'USER' AND p.name = 'PRODUCT_READ';
