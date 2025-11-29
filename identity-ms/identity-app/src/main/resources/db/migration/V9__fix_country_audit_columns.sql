ALTER TABLE identity.country ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE identity.country ADD COLUMN IF NOT EXISTS created_by VARCHAR(255) DEFAULT 'system';
ALTER TABLE identity.country ADD COLUMN IF NOT EXISTS last_modified_by VARCHAR(255) DEFAULT 'system';


ALTER TABLE identity.country ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';
ALTER TABLE identity.country ALTER COLUMN last_modified_at TYPE TIMESTAMPTZ USING last_modified_at AT TIME ZONE 'UTC';
