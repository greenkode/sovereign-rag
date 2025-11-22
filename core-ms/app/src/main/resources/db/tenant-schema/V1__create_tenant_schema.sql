-- Tenant database schema
-- This schema is applied to each tenant's isolated database
-- Database naming: compilot_tenant_<tenant_id>

-- NOTE: Document storage and embeddings are managed by LangChain4j's PgVectorEmbeddingStore
-- which automatically creates the 'langchain4j_embeddings' table

-- Enable required PostgreSQL extensions
CREATE EXTENSION IF NOT EXISTS vector;         -- pgvector for embeddings (used by LangChain4j)
CREATE EXTENSION IF NOT EXISTS pg_trgm;        -- Trigram for fuzzy text matching
CREATE EXTENSION IF NOT EXISTS btree_gin;      -- GIN indexes for JSONB

-- Chat sessions table
CREATE TABLE chat_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Session metadata
    persona VARCHAR(100) DEFAULT 'customer_service',
    language VARCHAR(10),

    -- State
    status VARCHAR(50) DEFAULT 'active',

    -- Timestamps
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    last_activity_at TIMESTAMP DEFAULT NOW(),
    closed_at TIMESTAMP,

    -- Session context
    context JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_chat_sessions_created ON chat_sessions(created_at DESC);
CREATE INDEX idx_chat_sessions_status ON chat_sessions(status);
CREATE INDEX idx_chat_sessions_last_activity ON chat_sessions(last_activity_at DESC);

-- Chat messages table
CREATE TABLE chat_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID REFERENCES chat_sessions(id) ON DELETE CASCADE,

    -- Message data
    role VARCHAR(50) NOT NULL CHECK (role IN ('user', 'assistant', 'system')),
    message TEXT NOT NULL,

    -- AI response metadata
    confidence_score INT,
    show_confidence BOOLEAN DEFAULT true,
    sources JSONB,

    -- Flags
    suggests_escalation BOOLEAN DEFAULT false,
    suggests_close BOOLEAN DEFAULT false,
    escalation_requested BOOLEAN DEFAULT false,

    -- Timestamps
    created_at TIMESTAMP DEFAULT NOW(),

    -- Message context
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_chat_messages_session ON chat_messages(session_id, created_at);
CREATE INDEX idx_chat_messages_role ON chat_messages(role);

-- Escalations table (customer support handoff)
CREATE TABLE escalations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID REFERENCES chat_sessions(id) ON DELETE SET NULL,

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
    updated_at TIMESTAMP DEFAULT NOW(),

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
    session_id UUID REFERENCES chat_sessions(id) ON DELETE SET NULL,

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

-- Feedback table (user ratings)
CREATE TABLE feedback (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID REFERENCES chat_sessions(id) ON DELETE SET NULL,
    message_id UUID REFERENCES chat_messages(id) ON DELETE SET NULL,

    -- Feedback data
    query TEXT NOT NULL,
    is_accurate BOOLEAN,
    rating INT CHECK (rating >= 1 AND rating <= 5),
    feedback_text TEXT,

    -- User info (optional)
    user_email VARCHAR(500),

    -- Timestamps
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_feedback_session ON feedback(session_id);
CREATE INDEX idx_feedback_is_accurate ON feedback(is_accurate);
CREATE INDEX idx_feedback_created ON feedback(created_at DESC);

-- Update triggers
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS update_chat_sessions_updated_at ON chat_sessions;
CREATE TRIGGER update_chat_sessions_updated_at
    BEFORE UPDATE ON chat_sessions
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_escalations_updated_at ON escalations;
CREATE TRIGGER update_escalations_updated_at
    BEFORE UPDATE ON escalations
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Comments for documentation
COMMENT ON TABLE chat_sessions IS 'Active and historical chat sessions';
COMMENT ON TABLE chat_messages IS 'Individual messages within chat sessions';
COMMENT ON TABLE escalations IS 'Customer support escalation requests';
COMMENT ON TABLE unanswered_queries IS 'Queries that could not be answered - used for content gap analysis';
COMMENT ON TABLE feedback IS 'User feedback on AI responses for quality monitoring';

-- Note: Document embeddings are stored in 'langchain4j_embeddings' table
-- which is automatically created and managed by LangChain4j's PgVectorEmbeddingStore
COMMENT ON EXTENSION vector IS 'pgvector extension for similarity search - used by LangChain4j';
