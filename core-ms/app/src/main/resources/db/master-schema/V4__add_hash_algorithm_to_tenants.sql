-- Add hash_algorithm column to track which hashing algorithm is used
-- Default to 'sha256' for existing tenants, new tenants will use 'bcrypt'
ALTER TABLE master.tenants
ADD COLUMN hash_algorithm VARCHAR(10) NOT NULL DEFAULT 'sha256';

-- Add comment for documentation
COMMENT ON COLUMN master.tenants.hash_algorithm IS 'Algorithm used for API key hashing: sha256 (legacy) or bcrypt (current)';
