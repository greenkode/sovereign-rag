ALTER TABLE identity.oauth_users ADD COLUMN IF NOT EXISTS registration_complete BOOLEAN DEFAULT FALSE;

UPDATE identity.oauth_users SET registration_complete = TRUE WHERE email_verified = TRUE;
