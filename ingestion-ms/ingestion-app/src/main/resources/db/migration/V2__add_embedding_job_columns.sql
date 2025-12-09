ALTER TABLE ingestion.ingestion_jobs ADD COLUMN IF NOT EXISTS parent_job_id UUID REFERENCES ingestion.ingestion_jobs(id);
ALTER TABLE ingestion.ingestion_jobs ADD COLUMN IF NOT EXISTS knowledge_source_id UUID;
ALTER TABLE ingestion.ingestion_jobs ADD COLUMN IF NOT EXISTS chunk_start_index INTEGER;
ALTER TABLE ingestion.ingestion_jobs ADD COLUMN IF NOT EXISTS chunk_end_index INTEGER;
ALTER TABLE ingestion.ingestion_jobs ADD COLUMN IF NOT EXISTS embeddings_created INTEGER NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_ingestion_jobs_parent ON ingestion.ingestion_jobs(parent_job_id);
CREATE INDEX IF NOT EXISTS idx_ingestion_jobs_knowledge_source ON ingestion.ingestion_jobs(knowledge_source_id);
CREATE INDEX IF NOT EXISTS idx_ingestion_jobs_embedding_queue ON ingestion.ingestion_jobs(status, job_type, priority DESC, created_at ASC)
    WHERE status = 'QUEUED' AND job_type = 'EMBEDDING';
