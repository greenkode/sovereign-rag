-- Increase the length of the event column in audit_log table
-- This prevents constraint violations for longer event descriptions

DO $$
BEGIN
    -- Check if the event column exists and alter its type
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'audit_log' AND column_name = 'event'
    ) THEN
        ALTER TABLE audit_log ALTER COLUMN event TYPE VARCHAR(255);
    END IF;
END
$$;