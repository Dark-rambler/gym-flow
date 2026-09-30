-- Caja (una abierta por gym) y pagos. FKs compuestas por gym_id (patrón de V4).

ALTER TABLE membership ADD CONSTRAINT uk_membership_gym_id UNIQUE (gym_id, id);

CREATE TABLE cash_session (
    id              BIGSERIAL PRIMARY KEY,
    gym_id          BIGINT        NOT NULL REFERENCES gym (id),
    status          VARCHAR(10)   NOT NULL CHECK (status IN ('OPEN', 'CLOSED')),
    opened_by       BIGINT        NOT NULL,
    opened_at       TIMESTAMPTZ   NOT NULL,
    opening_amount  NUMERIC(10,2) NOT NULL CHECK (opening_amount >= 0),
    closed_by       BIGINT,
    closed_at       TIMESTAMPTZ,
    counted_cash    NUMERIC(10,2) CHECK (counted_cash >= 0),
    expected_cash   NUMERIC(10,2),
    difference      NUMERIC(10,2),
    notes           VARCHAR(500),
    CONSTRAINT uk_cash_session_gym_id UNIQUE (gym_id, id),
    CONSTRAINT fk_cash_session_opened_by FOREIGN KEY (gym_id, opened_by) REFERENCES app_user (gym_id, id),
    CONSTRAINT fk_cash_session_closed_by FOREIGN KEY (gym_id, closed_by) REFERENCES app_user (gym_id, id),
    CONSTRAINT ck_cash_session_closed CHECK (
        (status = 'OPEN' AND closed_at IS NULL) OR
        (status = 'CLOSED' AND closed_at IS NOT NULL AND closed_by IS NOT NULL AND counted_cash IS NOT NULL))
);

-- una sola caja abierta por gimnasio
CREATE UNIQUE INDEX uk_cash_session_open ON cash_session (gym_id) WHERE status = 'OPEN';
CREATE INDEX idx_cash_session_gym_opened ON cash_session (gym_id, opened_at DESC);

CREATE TABLE payment (
    id               BIGSERIAL PRIMARY KEY,
    gym_id           BIGINT        NOT NULL REFERENCES gym (id),
    cash_session_id  BIGINT        NOT NULL,
    membership_id    BIGINT        NOT NULL,
    member_id        BIGINT        NOT NULL,
    amount           NUMERIC(10,2) NOT NULL CHECK (amount >= 0),
    method           VARCHAR(10)   NOT NULL CHECK (method IN ('CASH', 'YAPE', 'PLIN', 'CARD')),
    reference        VARCHAR(40),
    received_by      BIGINT        NOT NULL,
    paid_at          TIMESTAMPTZ   NOT NULL,
    idempotency_key  UUID          NOT NULL,
    voided_at        TIMESTAMPTZ,
    voided_by        BIGINT,
    void_reason      VARCHAR(200),
    CONSTRAINT uk_payment_idempotency UNIQUE (gym_id, idempotency_key),
    CONSTRAINT fk_payment_cash_session FOREIGN KEY (gym_id, cash_session_id) REFERENCES cash_session (gym_id, id),
    CONSTRAINT fk_payment_membership FOREIGN KEY (gym_id, membership_id) REFERENCES membership (gym_id, id),
    CONSTRAINT fk_payment_member FOREIGN KEY (gym_id, member_id) REFERENCES member (gym_id, id),
    CONSTRAINT fk_payment_received_by FOREIGN KEY (gym_id, received_by) REFERENCES app_user (gym_id, id),
    CONSTRAINT fk_payment_voided_by FOREIGN KEY (gym_id, voided_by) REFERENCES app_user (gym_id, id),
    CONSTRAINT ck_payment_void CHECK ((voided_at IS NULL) = (voided_by IS NULL))
);

CREATE INDEX idx_payment_session ON payment (cash_session_id);
CREATE INDEX idx_payment_membership ON payment (membership_id);
CREATE INDEX idx_payment_gym_paid_at ON payment (gym_id, paid_at);
