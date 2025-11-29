CREATE TABLE IF NOT EXISTS client
(
    id               UUID         NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id        VARCHAR(100) NOT NULL UNIQUE,
    client_name      VARCHAR(255) NOT NULL,
    email            VARCHAR(255) NOT NULL,
    company_name     VARCHAR(255),
    contact_person   VARCHAR(255),
    phone            VARCHAR(50),
    address          TEXT,
    country          VARCHAR(100),
    status           VARCHAR(50)  NOT NULL             DEFAULT 'ACTIVE',
    created_at       TIMESTAMP    NOT NULL             DEFAULT NOW(),
    last_modified_at TIMESTAMP    NOT NULL             DEFAULT NOW(),
    created_by       VARCHAR(100) NOT NULL             DEFAULT 'system',
    last_modified_by VARCHAR(100) NOT NULL             DEFAULT 'system',
    version          bigint       not null             default 0
);

CREATE INDEX idx_client_client_id ON client (client_id);
CREATE INDEX idx_client_email ON client (email);
CREATE INDEX idx_client_status ON client (status);

CREATE TABLE IF NOT EXISTS license
(
    id                   UUID         NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    license_key          TEXT         NOT NULL UNIQUE,
    client_id            VARCHAR(100) NOT NULL REFERENCES client (client_id),
    tier                 VARCHAR(50)  NOT NULL,
    max_tokens_per_month BIGINT       NOT NULL,
    max_tenants          INT          NOT NULL,
    features             TEXT[],
    issued_at            TIMESTAMP    NOT NULL             DEFAULT NOW(),
    expires_at           TIMESTAMP,
    revoked_at           TIMESTAMP,
    revoked_by           VARCHAR(100),
    revocation_reason    TEXT,
    status               VARCHAR(50)  NOT NULL             DEFAULT 'ACTIVE',
    metadata             JSONB,
    created_at           TIMESTAMP    NOT NULL             DEFAULT NOW(),
    last_modified_at     TIMESTAMP    NOT NULL             DEFAULT NOW(),
    created_by           VARCHAR(100) NOT NULL             DEFAULT 'system',
    last_modified_by     VARCHAR(100) NOT NULL             DEFAULT 'system',
    version              bigint       not null             default 0
);

CREATE INDEX idx_license_client_id ON license (client_id);
CREATE INDEX idx_license_status ON license (status);
CREATE INDEX idx_license_tier ON license (tier);
CREATE INDEX idx_license_expires_at ON license (expires_at);
CREATE INDEX idx_license_issued_at ON license (issued_at);

CREATE TABLE IF NOT EXISTS license_verification
(
    id                  UUID         NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    license_key_hash    VARCHAR(64)  NOT NULL,
    client_id           VARCHAR(100) NOT NULL,
    deployment_id       VARCHAR(255),
    ip_address          VARCHAR(50),
    hostname            VARCHAR(255),
    application_version VARCHAR(50),
    verification_time   TIMESTAMP    NOT NULL             DEFAULT NOW(),
    success             BOOLEAN      NOT NULL,
    failure_reason      TEXT,
    metadata            JSONB
);

CREATE INDEX idx_verification_license_hash ON license_verification (license_key_hash);
CREATE INDEX idx_verification_client_id ON license_verification (client_id);
CREATE INDEX idx_verification_time ON license_verification (verification_time);
CREATE INDEX idx_verification_deployment_id ON license_verification (deployment_id);

CREATE TABLE IF NOT EXISTS license_usage
(
    id               UUID         NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    license_key_hash VARCHAR(64)  NOT NULL,
    client_id        VARCHAR(100) NOT NULL,
    deployment_id    VARCHAR(255),
    report_date      DATE         NOT NULL,
    tokens_used      BIGINT       NOT NULL             DEFAULT 0,
    active_tenants   INT          NOT NULL             DEFAULT 0,
    active_users     INT          NOT NULL             DEFAULT 0,
    api_calls        BIGINT       NOT NULL             DEFAULT 0,
    metadata         JSONB,
    created_at       TIMESTAMP    NOT NULL             DEFAULT NOW(),
    UNIQUE (license_key_hash, deployment_id, report_date)
);

CREATE INDEX idx_usage_license_hash ON license_usage (license_key_hash);
CREATE INDEX idx_usage_client_id ON license_usage (client_id);
CREATE INDEX idx_usage_report_date ON license_usage (report_date);
CREATE INDEX idx_usage_deployment_id ON license_usage (deployment_id);

CREATE TABLE IF NOT EXISTS license_keys
(
    id               UUID         NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    key_type         VARCHAR(50)  NOT NULL,
    public_key       TEXT         NOT NULL,
    private_key      TEXT,
    active           BOOLEAN      NOT NULL             DEFAULT TRUE,
    created_at       TIMESTAMP    NOT NULL             DEFAULT NOW(),
    expires_at       TIMESTAMP,
    created_by       VARCHAR(100) NOT NULL             DEFAULT 'system',
    last_modified_by VARCHAR(100) NOT NULL             DEFAULT 'system',
    version          bigint       not null             default 0
);

CREATE INDEX idx_license_keys_active ON license_keys (active);
CREATE INDEX idx_license_keys_type ON license_keys (key_type);

COMMENT ON TABLE client IS 'Client information synced from oauth_registered_clients in identity-ms';
COMMENT ON TABLE license IS 'License records with keys and configurations';
COMMENT ON TABLE license_verification IS 'Log of license verification attempts';
COMMENT ON TABLE license_usage IS 'Usage metrics reported by deployments';
COMMENT ON TABLE license_keys IS 'RSA key pairs for license signing';
