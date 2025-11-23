-- Update all trust_level values in oauth_users table to TIER_THREE
-- This will update both NULL and existing values to ensure consistency with TrustLevel enum
UPDATE oauth_users
SET trust_level = 'TIER_THREE';