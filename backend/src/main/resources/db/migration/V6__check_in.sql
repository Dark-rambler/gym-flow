-- Registro de entradas (permitidas y denegadas). member_id es NULL si el código no correspondía a ningún socio.

CREATE TABLE check_in (
    id             BIGSERIAL PRIMARY KEY,
    gym_id         BIGINT      NOT NULL REFERENCES gym (id),
    member_id      BIGINT,
    membership_id  BIGINT,
    method         VARCHAR(5)  NOT NULL CHECK (method IN ('QR', 'DNI')),
    result         VARCHAR(10) NOT NULL CHECK (result IN ('ALLOWED', 'DENIED')),
    reason         VARCHAR(20),
    checked_at     TIMESTAMPTZ NOT NULL,
    checked_by     BIGINT      NOT NULL,
    CONSTRAINT fk_check_in_member     FOREIGN KEY (gym_id, member_id)     REFERENCES member (gym_id, id),
    CONSTRAINT fk_check_in_membership FOREIGN KEY (gym_id, membership_id) REFERENCES membership (gym_id, id),
    CONSTRAINT fk_check_in_checked_by FOREIGN KEY (gym_id, checked_by)    REFERENCES app_user (gym_id, id),
    CONSTRAINT ck_check_in_reason CHECK ((result = 'ALLOWED') = (reason IS NULL))
);

CREATE INDEX idx_check_in_gym_at ON check_in (gym_id, checked_at DESC);
CREATE INDEX idx_check_in_member_at ON check_in (member_id, checked_at DESC);
