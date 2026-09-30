-- Tenants (gimnasios) y usuarios del staff.
-- Toda tabla de negocio lleva gym_id NOT NULL + índice; el aislamiento lo aplica Hibernate @TenantId.

CREATE TABLE gym (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(120) NOT NULL,
    slug        VARCHAR(60)  NOT NULL UNIQUE,
    timezone    VARCHAR(40)  NOT NULL DEFAULT 'America/Lima',
    currency    CHAR(3)      NOT NULL DEFAULT 'PEN',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE app_user (
    id             BIGSERIAL PRIMARY KEY,
    gym_id         BIGINT       NOT NULL REFERENCES gym (id),
    email          VARCHAR(160) NOT NULL,
    password_hash  VARCHAR(100) NOT NULL,
    full_name      VARCHAR(120) NOT NULL,
    role           VARCHAR(20)  NOT NULL CHECK (role IN ('OWNER', 'ADMIN', 'RECEPTIONIST')),
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_app_user_email UNIQUE (email)
);

CREATE INDEX idx_app_user_gym ON app_user (gym_id);
