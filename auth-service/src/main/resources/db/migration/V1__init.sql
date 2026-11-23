CREATE TABLE tenants (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(150) NOT NULL,
    code       VARCHAR(50)  NOT NULL UNIQUE,
    created_at TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    tenant_id     BIGINT       NOT NULL REFERENCES tenants (id),
    username      VARCHAR(80)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    full_name     VARCHAR(150) NOT NULL,
    phone         VARCHAR(20),
    role          VARCHAR(20)  NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT now(),
    CONSTRAINT uq_user_tenant_username UNIQUE (tenant_id, username)
);