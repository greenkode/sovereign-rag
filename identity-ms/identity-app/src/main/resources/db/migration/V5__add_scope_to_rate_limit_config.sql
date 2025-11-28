ALTER TABLE identity.rate_limit_config ADD COLUMN IF NOT EXISTS scope VARCHAR(20) NOT NULL DEFAULT 'INDIVIDUAL';

DROP INDEX IF EXISTS identity.idx_rate_limit_method_tier;

ALTER TABLE identity.rate_limit_config DROP CONSTRAINT IF EXISTS rate_limit_config_method_name_subscription_tier_key;

ALTER TABLE identity.rate_limit_config ADD CONSTRAINT rate_limit_config_method_tier_scope_key UNIQUE (method_name, subscription_tier, scope);

CREATE INDEX IF NOT EXISTS idx_rate_limit_method_tier_scope ON identity.rate_limit_config(method_name, subscription_tier, scope, active);

UPDATE identity.rate_limit_config SET scope = 'ORGANIZATION' WHERE method_name = 'api-request';
