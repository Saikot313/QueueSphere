CREATE TABLE service_queues (
    id                  BIGSERIAL PRIMARY KEY,
    tenant_id           BIGINT       NOT NULL,
    name                VARCHAR(150) NOT NULL,
    code                VARCHAR(30)  NOT NULL,
    active              BOOLEAN      NOT NULL DEFAULT TRUE,
    avg_service_seconds INT          NOT NULL DEFAULT 300,
    version             BIGINT       NOT NULL DEFAULT 0,
    created_at          TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT uq_queue_tenant_code UNIQUE (tenant_id, code)
);

CREATE TABLE queue_tokens (
    id            BIGSERIAL PRIMARY KEY,
    tenant_id     BIGINT       NOT NULL,
    queue_id      BIGINT       NOT NULL REFERENCES service_queues (id),
    token_number  INT          NOT NULL,
    token_date    DATE         NOT NULL,
    citizen_name  VARCHAR(150) NOT NULL,
    phone         VARCHAR(20),
    priority      VARCHAR(20)  NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    counter_no    INT,
    issued_at     TIMESTAMP    NOT NULL,
    called_at     TIMESTAMP,
    started_at    TIMESTAMP,
    completed_at  TIMESTAMP,
    CONSTRAINT uq_token_per_day UNIQUE (queue_id, token_date, token_number)
);

CREATE INDEX idx_tokens_status_date ON queue_tokens (status, token_date);
CREATE INDEX idx_tokens_tenant ON queue_tokens (tenant_id);