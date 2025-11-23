-- Update audit_log table schema to match new AuditLogEntity structure

-- Add new columns only if they don't exist
DO $$
BEGIN
    -- Add actor_id column if it doesn't exist
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'audit_log' AND column_name = 'actor_id'
    ) THEN
        ALTER TABLE audit_log ADD COLUMN actor_id VARCHAR(255);
    END IF;

    -- Add actor_name column if it doesn't exist
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'audit_log' AND column_name = 'actor_name'
    ) THEN
        ALTER TABLE audit_log ADD COLUMN actor_name VARCHAR(255);
    END IF;

    -- Add merchant_id column if it doesn't exist
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'audit_log' AND column_name = 'merchant_id'
    ) THEN
        ALTER TABLE audit_log ADD COLUMN merchant_id VARCHAR(255);
    END IF;
END
$$;

-- Populate new columns with data from existing columns where possible
-- Only update if columns are still null (handles re-runs)

-- For actor_id, use the existing identity column value if identity column still exists
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'audit_log' AND column_name = 'identity'
    ) THEN
        UPDATE audit_log SET actor_id = identity WHERE actor_id IS NULL;
    END IF;
END
$$;

-- For actor_name, set a default value only for null values
UPDATE audit_log SET actor_name = 'Unknown Actor' WHERE actor_name IS NULL;

-- For merchant_id, set a default value only for null values
UPDATE audit_log SET merchant_id = 'unknown' WHERE merchant_id IS NULL;

-- Make the new columns non-nullable after populating them (only if they're currently nullable)
DO $$
BEGIN
    -- Set actor_id as NOT NULL if it's currently nullable
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'audit_log' AND column_name = 'actor_id' AND is_nullable = 'YES'
    ) THEN
        ALTER TABLE audit_log ALTER COLUMN actor_id SET NOT NULL;
    END IF;

    -- Set actor_name as NOT NULL if it's currently nullable
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'audit_log' AND column_name = 'actor_name' AND is_nullable = 'YES'
    ) THEN
        ALTER TABLE audit_log ALTER COLUMN actor_name SET NOT NULL;
    END IF;

    -- Set merchant_id as NOT NULL if it's currently nullable
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'audit_log' AND column_name = 'merchant_id' AND is_nullable = 'YES'
    ) THEN
        ALTER TABLE audit_log ALTER COLUMN merchant_id SET NOT NULL;
    END IF;
END
$$;

-- Remove the old identity column only if it exists
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'audit_log' AND column_name = 'identity'
    ) THEN
        ALTER TABLE audit_log DROP COLUMN identity;
    END IF;
END
$$;