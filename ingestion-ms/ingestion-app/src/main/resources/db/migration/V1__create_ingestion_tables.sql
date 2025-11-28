CREATE TABLE IF NOT EXISTS ingestion_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    knowledge_base_id UUID,
    job_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    source_type VARCHAR(50),
    source_reference TEXT,
    file_name VARCHAR(500),
    file_size BIGINT,
    mime_type VARCHAR(255),
    progress INT DEFAULT 0,
    error_message TEXT,
    retry_count INT DEFAULT 0,
    priority INT DEFAULT 0,
    chunks_created INT,
    bytes_processed BIGINT,
    started_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    processing_duration_ms BIGINT,
    locked_at TIMESTAMP WITH TIME ZONE,
    locked_by VARCHAR(255),
    created_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    last_modified_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_ingestion_jobs_tenant_id ON ingestion_jobs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_ingestion_jobs_status ON ingestion_jobs(status);
CREATE INDEX IF NOT EXISTS idx_ingestion_jobs_knowledge_base_id ON ingestion_jobs(knowledge_base_id);
CREATE INDEX IF NOT EXISTS idx_ingestion_jobs_tenant_status ON ingestion_jobs(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_ingestion_jobs_queue ON ingestion_jobs(status, priority DESC, created_date ASC) WHERE status = 'QUEUED';

CREATE TABLE IF NOT EXISTS ingestion_job_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL REFERENCES ingestion_jobs(id) ON DELETE CASCADE,
    item_type VARCHAR(50) NOT NULL,
    source_url TEXT,
    file_path TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    retry_count INT DEFAULT 0,
    processing_order INT DEFAULT 0,
    created_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    last_modified_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_ingestion_job_items_job_id ON ingestion_job_items(job_id);
CREATE INDEX IF NOT EXISTS idx_ingestion_job_items_status ON ingestion_job_items(status);

CREATE TABLE IF NOT EXISTS tenant_quotas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL UNIQUE,
    tier VARCHAR(50) NOT NULL DEFAULT 'TRIAL',
    storage_used_bytes BIGINT DEFAULT 0,
    jobs_this_month INT DEFAULT 0,
    month_year VARCHAR(7),
    created_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    last_modified_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_tenant_quotas_tenant_id ON tenant_quotas(tenant_id);
