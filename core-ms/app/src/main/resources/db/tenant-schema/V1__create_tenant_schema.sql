-- Tenant database schema
-- This schema is applied to each tenant's isolated database
-- Database naming: sovereignrag_tenant_<tenant_id>

-- NOTE: Document storage and embeddings are managed by LangChain4j's PgVectorEmbeddingStore
-- which automatically creates the 'langchain4j_embeddings' table

-- Enable required PostgreSQL extensions
CREATE EXTENSION IF NOT EXISTS vector;         -- pgvector for embeddings (used by LangChain4j)
CREATE EXTENSION IF NOT EXISTS pg_trgm;        -- Trigram for fuzzy text matching
CREATE EXTENSION IF NOT EXISTS btree_gin;      -- GIN indexes for JSONB

-- Escalations table (customer support handoff)
CREATE TABLE escalations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID,

    -- Escalation details
    reason TEXT,
    status VARCHAR(50) DEFAULT 'pending',
    priority VARCHAR(20) DEFAULT 'normal',

    -- User contact info
    user_email VARCHAR(500),
    user_name VARCHAR(500),
    user_phone VARCHAR(100),

    -- Assignment
    assigned_to VARCHAR(255),
    assigned_at TIMESTAMP,

    -- Resolution
    resolved_at TIMESTAMP,
    resolution_notes TEXT,

    -- Timestamps
    created_at TIMESTAMP DEFAULT NOW(),
    last_modified_at TIMESTAMP DEFAULT NOW(),

    -- Additional data
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_escalations_session ON escalations(session_id);
CREATE INDEX idx_escalations_status ON escalations(status);
CREATE INDEX idx_escalations_created ON escalations(created_at DESC);

-- Unanswered queries table (for content gap analysis)
CREATE TABLE unanswered_queries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Query details
    query TEXT NOT NULL,
    language VARCHAR(10),

    -- Context
    session_id UUID,

    -- Analysis
    confidence_score DOUBLE PRECISION,
    reason VARCHAR(255),

    -- Resolution tracking
    status VARCHAR(50) DEFAULT 'open',
    resolved_at TIMESTAMP,
    resolution_notes TEXT,

    -- Timestamps
    created_at TIMESTAMP DEFAULT NOW(),

    -- Count occurrences
    occurrence_count INT DEFAULT 1,
    last_occurred_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_unanswered_queries_query ON unanswered_queries(query);
CREATE INDEX idx_unanswered_queries_status ON unanswered_queries(status);
CREATE INDEX idx_unanswered_queries_created ON unanswered_queries(created_at DESC);

-- Update triggers
CREATE OR REPLACE FUNCTION update_last_modified_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.last_modified_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS update_escalations_last_modified_at ON escalations;
CREATE TRIGGER update_escalations_last_modified_at
    BEFORE UPDATE ON escalations
    FOR EACH ROW
    EXECUTE FUNCTION update_last_modified_at_column();

-- Comments for documentation
COMMENT ON TABLE escalations IS 'Customer support escalation requests';
COMMENT ON TABLE unanswered_queries IS 'Queries that could not be answered - used for content gap analysis';

-- Note: Document embeddings are stored in 'langchain4j_embeddings' table
-- which is automatically created and managed by LangChain4j's PgVectorEmbeddingStore
COMMENT ON EXTENSION vector IS 'pgvector extension for similarity search - used by LangChain4j';
