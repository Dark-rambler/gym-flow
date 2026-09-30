-- Socios, planes y membresías. Todas las tablas son de negocio: gym_id + @TenantId.

CREATE TABLE member (
    id          BIGSERIAL PRIMARY KEY,
    gym_id      BIGINT       NOT NULL REFERENCES gym (id),
    full_name   VARCHAR(120) NOT NULL,
    dni         VARCHAR(12)  NOT NULL,
    phone       VARCHAR(20),
    email       VARCHAR(160),
    birth_date  DATE,
    notes       VARCHAR(500),
    qr_token    UUID         NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_member_gym_dni UNIQUE (gym_id, dni),
    CONSTRAINT uk_member_qr_token UNIQUE (qr_token)
);

CREATE INDEX idx_member_gym ON member (gym_id);
CREATE INDEX idx_member_gym_name ON member (gym_id, lower(full_name));

CREATE TABLE membership_plan (
    id             BIGSERIAL PRIMARY KEY,
    gym_id         BIGINT        NOT NULL REFERENCES gym (id),
    name           VARCHAR(80)   NOT NULL,
    duration_days  INT           NOT NULL CHECK (duration_days BETWEEN 1 AND 730),
    price          NUMERIC(10,2) NOT NULL CHECK (price >= 0),
    active         BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uk_plan_gym_name UNIQUE (gym_id, name)
);

CREATE INDEX idx_membership_plan_gym ON membership_plan (gym_id);

-- plan_name y price son una foto del plan al momento de la venta.
-- status guarda solo ACTIVE | FROZEN | CANCELLED; SCHEDULED/EXPIRED se derivan de las fechas.
CREATE TABLE membership (
    id            BIGSERIAL PRIMARY KEY,
    gym_id        BIGINT        NOT NULL REFERENCES gym (id),
    member_id     BIGINT        NOT NULL REFERENCES member (id),
    plan_id       BIGINT        NOT NULL REFERENCES membership_plan (id),
    plan_name     VARCHAR(80)   NOT NULL,
    price         NUMERIC(10,2) NOT NULL CHECK (price >= 0),
    start_date    DATE          NOT NULL,
    end_date      DATE          NOT NULL,
    status        VARCHAR(12)   NOT NULL CHECK (status IN ('ACTIVE', 'FROZEN', 'CANCELLED')),
    frozen_since  DATE,
    frozen_days   INT           NOT NULL DEFAULT 0,
    created_by    BIGINT        NOT NULL REFERENCES app_user (id),
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_membership_dates CHECK (end_date >= start_date)
);

CREATE INDEX idx_membership_gym ON membership (gym_id);
CREATE INDEX idx_membership_member_end ON membership (member_id, end_date);
CREATE INDEX idx_membership_plan ON membership (plan_id);
