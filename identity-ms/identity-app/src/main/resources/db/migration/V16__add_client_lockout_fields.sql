-- Add lockout tracking fields to oauth_registered_clients table
ALTER TABLE identity.oauth_registered_clients 
ADD COLUMN IF NOT EXISTS failed_auth_attempts INTEGER NOT NULL DEFAULT 0,
ADD COLUMN IF NOT EXISTS locked_until TIMESTAMP,
ADD COLUMN IF NOT EXISTS last_failed_auth TIMESTAMP;

-- Add index for efficient client lockout queries
CREATE INDEX IF NOT EXISTS idx_oauth_clients_lockout 
ON identity.oauth_registered_clients (client_id, locked_until, failed_auth_attempts);