-- Add lockout tracking fields to oauth_users table
ALTER TABLE oauth_users
ADD COLUMN IF NOT EXISTS failed_login_attempts INTEGER NOT NULL DEFAULT 0,
ADD COLUMN IF NOT EXISTS locked_until TIMESTAMP,
ADD COLUMN IF NOT EXISTS last_failed_login TIMESTAMP;

-- Add index for efficient lockout queries
CREATE INDEX IF NOT EXISTS idx_oauth_users_lockout 
ON oauth_users (username, locked_until, failed_login_attempts);