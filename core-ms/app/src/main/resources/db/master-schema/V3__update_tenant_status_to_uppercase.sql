-- Migration to update tenant status values from lowercase to uppercase
-- This aligns the database constraint with the JPA enum (ACTIVE, SUSPENDED, DELETED)

SET search_path TO master;

-- 1. Drop the old check constraint FIRST (before updating data)
ALTER TABLE tenants DROP CONSTRAINT IF EXISTS tenants_status_check;

-- 2. Update existing data from lowercase to uppercase
UPDATE tenants SET status = 'ACTIVE' WHERE status = 'active';
UPDATE tenants SET status = 'SUSPENDED' WHERE status = 'suspended';
UPDATE tenants SET status = 'DELETED' WHERE status = 'deleted';

-- 3. Add new check constraint with uppercase values
ALTER TABLE tenants ADD CONSTRAINT tenants_status_check
    CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED'));

-- 4. Update the default value to uppercase
ALTER TABLE tenants ALTER COLUMN status SET DEFAULT 'ACTIVE';
