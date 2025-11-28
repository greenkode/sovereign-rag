CREATE TABLE IF NOT EXISTS identity.rate_limit_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    method_name VARCHAR(100) NOT NULL,
    subscription_tier VARCHAR(50) NOT NULL,
    capacity INT NOT NULL,
    time_value INT NOT NULL,
    time_unit VARCHAR(20) NOT NULL,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(method_name, subscription_tier)
);

CREATE INDEX IF NOT EXISTS idx_rate_limit_method_tier ON identity.rate_limit_config(method_name, subscription_tier, active);
