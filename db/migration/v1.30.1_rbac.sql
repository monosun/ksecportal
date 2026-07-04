-- v1.30.1: RBAC 권한관리 테이블

CREATE TABLE IF NOT EXISTS custom_roles (
    id          BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at  DATETIME,
    updated_at  DATETIME
);

CREATE TABLE IF NOT EXISTS role_permissions (
    id          BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    role_id     BIGINT      NOT NULL,
    menu_key    VARCHAR(100) NOT NULL,
    can_read    BOOLEAN     NOT NULL DEFAULT FALSE,
    can_write   BOOLEAN     NOT NULL DEFAULT FALSE,
    can_delete  BOOLEAN     NOT NULL DEFAULT FALSE,
    UNIQUE KEY uq_role_menu (role_id, menu_key),
    FOREIGN KEY (role_id) REFERENCES custom_roles(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS user_custom_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES custom_roles(id) ON DELETE CASCADE
);
