ALTER TABLE ingestion.ingestion_jobs RENAME COLUMN created_at TO created_date;
ALTER TABLE ingestion.ingestion_jobs RENAME COLUMN updated_at TO last_modified_date;
ALTER TABLE ingestion.ingestion_jobs RENAME COLUMN updated_by TO last_modified_by;
ALTER TABLE ingestion.ingestion_jobs ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE ingestion.ingestion_job_items RENAME COLUMN created_at TO created_date;
ALTER TABLE ingestion.ingestion_job_items RENAME COLUMN updated_at TO last_modified_date;
ALTER TABLE ingestion.ingestion_job_items RENAME COLUMN updated_by TO last_modified_by;
ALTER TABLE ingestion.ingestion_job_items RENAME COLUMN source_key TO source_reference;
ALTER TABLE ingestion.ingestion_job_items ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE ingestion.tenant_quotas RENAME COLUMN created_at TO created_date;
ALTER TABLE ingestion.tenant_quotas RENAME COLUMN updated_at TO last_modified_date;
ALTER TABLE ingestion.tenant_quotas RENAME COLUMN updated_by TO last_modified_by;
ALTER TABLE ingestion.tenant_quotas ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
