CREATE TABLE IF NOT EXISTS knowledge_sources (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    knowledge_base_id VARCHAR(255) NOT NULL REFERENCES knowledge_base(id),
    source_type VARCHAR(50) NOT NULL,
    file_name VARCHAR(500),
    source_url VARCHAR(2000),
    title VARCHAR(500),
    mime_type VARCHAR(100),
    file_size BIGINT,
    s3_key VARCHAR(500),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    error_message VARCHAR(2000),
    chunk_count INT NOT NULL DEFAULT 0,
    embedding_count INT NOT NULL DEFAULT 0,
    ingestion_job_id UUID,
    metadata JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_knowledge_sources_kb_id ON knowledge_sources(knowledge_base_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_sources_status ON knowledge_sources(knowledge_base_id, status);
CREATE INDEX IF NOT EXISTS idx_knowledge_sources_ingestion_job ON knowledge_sources(ingestion_job_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_sources_created ON knowledge_sources(knowledge_base_id, created_at DESC);
