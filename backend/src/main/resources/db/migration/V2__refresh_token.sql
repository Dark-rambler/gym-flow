-- Refresh tokens opacos (solo se guarda el hash SHA-256). Tabla de infraestructura de auth:
-- se accede siempre en contexto de sistema (login/refresh/logout), por eso no usa @TenantId.

CREATE TABLE refresh_token (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES app_user (id),
    gym_id      BIGINT      NOT NULL REFERENCES gym (id),
    token_hash  CHAR(64)    NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_refresh_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_token_user ON refresh_token (user_id);
CREATE INDEX idx_refresh_token_gym ON refresh_token (gym_id);
