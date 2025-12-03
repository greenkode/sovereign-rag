CREATE SCHEMA IF NOT EXISTS ingestion;

CREATE TABLE IF NOT EXISTS ingestion.ingestion_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    knowledge_base_id UUID,
    job_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    source_type VARCHAR(50),
    source_reference VARCHAR(2000),
    file_name VARCHAR(500),
    file_size BIGINT,
    mime_type VARCHAR(100),
    progress INTEGER NOT NULL DEFAULT 0,
    error_message VARCHAR(2000),
    retry_count INTEGER NOT NULL DEFAULT 0,
    max_retries INTEGER NOT NULL DEFAULT 3,
    metadata JSONB,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    processing_duration_ms BIGINT,
    chunks_created INTEGER NOT NULL DEFAULT 0,
    bytes_processed BIGINT NOT NULL DEFAULT 0,
    priority INTEGER NOT NULL DEFAULT 0,
    locked_at TIMESTAMPTZ,
    locked_by VARCHAR(255),
    visible_after TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_modified_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS ingestion.ingestion_job_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL REFERENCES ingestion.ingestion_jobs(id) ON DELETE CASCADE,
    item_index INTEGER NOT NULL,
    file_name VARCHAR(500) NOT NULL,
    file_size BIGINT,
    mime_type VARCHAR(100),
    source_reference VARCHAR(2000),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    error_message VARCHAR(2000),
    chunks_created INTEGER NOT NULL DEFAULT 0,
    bytes_processed BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_modified_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS ingestion.organization_quotas (
    organization_id UUID PRIMARY KEY,
    tier VARCHAR(50) NOT NULL DEFAULT 'FREE',
    storage_used_bytes BIGINT NOT NULL DEFAULT 0,
    storage_quota_bytes BIGINT NOT NULL DEFAULT 104857600,
    monthly_jobs_used INTEGER NOT NULL DEFAULT 0,
    monthly_job_limit INTEGER NOT NULL DEFAULT 50,
    current_period_start TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_job_completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_modified_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_ingestion_jobs_organization_id ON ingestion.ingestion_jobs(organization_id);
CREATE INDEX IF NOT EXISTS idx_ingestion_jobs_status ON ingestion.ingestion_jobs(status);
CREATE INDEX IF NOT EXISTS idx_ingestion_jobs_organization_status ON ingestion.ingestion_jobs(organization_id, status);
CREATE INDEX IF NOT EXISTS idx_ingestion_jobs_knowledge_base ON ingestion.ingestion_jobs(knowledge_base_id);
CREATE INDEX IF NOT EXISTS idx_ingestion_jobs_created_at ON ingestion.ingestion_jobs(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_ingestion_jobs_queue ON ingestion.ingestion_jobs(status, priority DESC, created_at ASC)
    WHERE status = 'QUEUED';

CREATE INDEX IF NOT EXISTS idx_ingestion_jobs_visible_after ON ingestion.ingestion_jobs(visible_after)
    WHERE status = 'QUEUED' AND visible_after IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_ingestion_jobs_stale_locks ON ingestion.ingestion_jobs(locked_at)
    WHERE status = 'PROCESSING';

CREATE INDEX IF NOT EXISTS idx_ingestion_job_items_job_id ON ingestion.ingestion_job_items(job_id);
CREATE INDEX IF NOT EXISTS idx_ingestion_job_items_status ON ingestion.ingestion_job_items(job_id, status);
