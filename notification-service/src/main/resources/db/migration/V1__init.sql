CREATE TABLE notifications (
    id          BIGSERIAL PRIMARY KEY,
    tenant_id   BIGINT      NOT NULL,
    token_id    BIGINT,
    phone       VARCHAR(20) NOT NULL,
    event_type  VARCHAR(30) NOT NULL,
    message     TEXT        NOT NULL,
    status      VARCHAR(20) NOT NULL,
    created_at  TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE INDEX idx_notif_tenant ON notifications (tenant_id, id DESC);