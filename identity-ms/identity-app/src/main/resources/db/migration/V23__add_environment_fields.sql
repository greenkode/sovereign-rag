ALTER TABLE identity.oauth_users
ADD COLUMN environment_preference VARCHAR(20) NOT NULL DEFAULT 'SANDBOX',
ADD COLUMN environment_last_switched_at TIMESTAMP;

ALTER TABLE identity.oauth_registered_clients
ADD COLUMN environment_mode VARCHAR(20) NOT NULL DEFAULT 'SANDBOX';

CREATE INDEX IF NOT EXISTS idx_oauth_users_environment ON identity.oauth_users(environment_preference);
CREATE INDEX IF NOT EXISTS idx_oauth_clients_environment ON identity.oauth_registered_clients(environment_mode);

COMMENT ON COLUMN identity.oauth_users.environment_preference IS 'User preference for which environment to view: SANDBOX or PRODUCTION';
COMMENT ON COLUMN identity.oauth_users.environment_last_switched_at IS 'Timestamp of last environment switch by user';
COMMENT ON COLUMN identity.oauth_registered_clients.environment_mode IS 'Business environment mode: SANDBOX or PRODUCTION. Only super admin can change to PRODUCTION';
