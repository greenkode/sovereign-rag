ALTER TABLE core.tenants ADD COLUMN IF NOT EXISTS client_id VARCHAR(100);

ALTER TABLE core.tenants DROP COLUMN IF EXISTS subscription_tier;

CREATE INDEX IF NOT EXISTS idx_tenants_client_id ON core.tenants (client_id);
