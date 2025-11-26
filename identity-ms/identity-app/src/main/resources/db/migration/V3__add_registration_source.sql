ALTER TABLE identity.oauth_users ADD COLUMN IF NOT EXISTS registration_source VARCHAR(50) DEFAULT 'INVITATION';

CREATE INDEX IF NOT EXISTS idx_oauth_users_registration_source ON identity.oauth_users(registration_source);
