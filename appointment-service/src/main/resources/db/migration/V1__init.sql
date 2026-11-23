CREATE TABLE appointment_slots (
    id         BIGSERIAL PRIMARY KEY,
    tenant_id  BIGINT    NOT NULL,
    queue_id   BIGINT    NOT NULL,
    slot_time  TIMESTAMP NOT NULL,
    capacity   INT       NOT NULL,
    booked     INT       NOT NULL DEFAULT 0,
    CONSTRAINT uq_slot UNIQUE (tenant_id, queue_id, slot_time)
);

CREATE TABLE appointments (
    id            BIGSERIAL PRIMARY KEY,
    tenant_id     BIGINT       NOT NULL,
    queue_id      BIGINT       NOT NULL,
    user_id       BIGINT       NOT NULL,
    citizen_name  VARCHAR(150) NOT NULL,
    phone         VARCHAR(20),
    slot_time     TIMESTAMP    NOT NULL,
    priority      VARCHAR(20)  NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    token_id      BIGINT,
    token_number  INT,
    created_at    TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_appt_tenant_user ON appointments (tenant_id, user_id);
CREATE INDEX idx_appt_tenant_queue ON appointments (tenant_id, queue_id);