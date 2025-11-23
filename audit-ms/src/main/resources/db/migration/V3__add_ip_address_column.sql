-- Add ip_address column to audit_log table

-- Add ip_address column if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'audit_log' AND column_name = 'ip_address'
    ) THEN
        ALTER TABLE audit_log ADD COLUMN ip_address VARCHAR(45) DEFAULT 'N/A';
    END IF;
END
$$;

-- Update existing records to have default IP address value only for null values
UPDATE audit_log SET ip_address = 'N/A' WHERE ip_address IS NULL;

-- Make the ip_address column non-nullable after populating it
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'audit_log' AND column_name = 'ip_address' AND is_nullable = 'YES'
    ) THEN
        ALTER TABLE audit_log ALTER COLUMN ip_address SET NOT NULL;
    END IF;
END
$$;