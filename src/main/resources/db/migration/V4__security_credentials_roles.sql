-- ==============================================================================
-- PulsePass — V4: Capa de Seguridad (Credenciales y Roles)
-- ==============================================================================

-- 1. Tabla de credenciales (1:1 con users)
CREATE TABLE user_credentials (
    user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    password_hash VARCHAR(255) NOT NULL,
    password_changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Tabla de roles (N:1 con users, relación con clave primaria compuesta)
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL CHECK (role IN ('USER', 'STAFF', 'ADMIN')),
    PRIMARY KEY (user_id, role)
);

-- 3. Índices de optimización para carga de roles y seguridad
CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);

-- Nota de migración para usuarios preexistentes (legados):
-- No se generan contraseñas predeterminadas ni compartidas para usuarios legados.
-- Los registros legados sin credenciales permanecerán sin posibilidad de iniciar sesión
-- hasta que se ejecute un procedimiento de activación seguro.
