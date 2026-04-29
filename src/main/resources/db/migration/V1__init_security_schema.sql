-- Permissions
CREATE TABLE permissions (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    created_by  VARCHAR(100) NULL,
    updated_by  VARCHAR(100) NULL,
    version     BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_permissions_name (name)
);

-- Roles
CREATE TABLE roles (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50)  NOT NULL,
    description VARCHAR(255) NULL,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    created_by  VARCHAR(100) NULL,
    updated_by  VARCHAR(100) NULL,
    version     BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_roles_name (name)
);

-- Users
CREATE TABLE users (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    username      VARCHAR(50)   NOT NULL,
    email         VARCHAR(150)  NOT NULL,
    password_hash VARCHAR(100)  NOT NULL,
    first_name    VARCHAR(100)  NULL,
    last_name     VARCHAR(100)  NULL,
    enabled       BIT(1)        NOT NULL DEFAULT b'1',
    created_at    DATETIME(6)   NOT NULL,
    updated_at    DATETIME(6)   NOT NULL,
    created_by    VARCHAR(100)  NULL,
    updated_by    VARCHAR(100)  NULL,
    version       BIGINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username),
    UNIQUE KEY uk_users_email (email)
);

-- Join tables
CREATE TABLE role_permissions (
    role_id       BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role       FOREIGN KEY (role_id)       REFERENCES roles (id)       ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions (id) ON DELETE CASCADE
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
);

-- Seed permissions
INSERT INTO permissions (name, description, created_at, updated_at, created_by, updated_by) VALUES
    ('USER_READ',   'Read users',   NOW(6), NOW(6), 'system', 'system'),
    ('USER_WRITE',  'Modify users', NOW(6), NOW(6), 'system', 'system'),
    ('USER_DELETE', 'Delete users', NOW(6), NOW(6), 'system', 'system'),
    ('ROLE_READ',   'Read roles',   NOW(6), NOW(6), 'system', 'system'),
    ('ROLE_WRITE',  'Modify roles', NOW(6), NOW(6), 'system', 'system');

-- Seed roles
INSERT INTO roles (name, description, created_at, updated_at, created_by, updated_by) VALUES
    ('ADMIN',   'Administrator with all privileges', NOW(6), NOW(6), 'system', 'system'),
    ('MANAGER', 'Can read and write user data',      NOW(6), NOW(6), 'system', 'system'),
    ('USER',    'Standard end-user',                 NOW(6), NOW(6), 'system', 'system');

-- Wire role -> permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ADMIN';

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'MANAGER' AND p.name IN ('USER_READ', 'USER_WRITE', 'ROLE_READ');
