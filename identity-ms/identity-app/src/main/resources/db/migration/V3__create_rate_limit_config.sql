CREATE TABLE IF NOT EXISTS identity.rate_limit_config
(
    id                 UUID PRIMARY KEY               DEFAULT gen_random_uuid(),
    method_name        VARCHAR(100)          NOT NULL,
    subscription_tier  VARCHAR(50)           NOT NULL,
    scope              VARCHAR(50)           NOT NULL DEFAULT 'INDIVIDUAL',
    capacity           INT                   NOT NULL,
    time_value         INT                   NOT NULL,
    time_unit          VARCHAR(20)           NOT NULL,
    active             BOOLEAN                        DEFAULT true,
    created_by         VARCHAR(255)                   DEFAULT 'system',
    created_at       TIMESTAMPTZ                    DEFAULT NOW(),
    last_modified_by   VARCHAR(255)                   DEFAULT 'system',
    last_modified_at TIMESTAMPTZ                    DEFAULT NOW(),
    version            BIGINT                NOT NULL DEFAULT 0,
    UNIQUE (method_name, subscription_tier)
);

CREATE INDEX IF NOT EXISTS idx_rate_limit_method_tier ON identity.rate_limit_config (method_name, subscription_tier, active);
