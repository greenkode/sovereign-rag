CREATE TABLE IF NOT EXISTS remi_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    knowledge_base_id UUID NOT NULL,
    query_id UUID NOT NULL,
    retrieval_metrics_id UUID REFERENCES retrieval_metrics(id),
    query_text TEXT NOT NULL,
    generated_answer TEXT,
    answer_relevance_score DOUBLE PRECISION,
    answer_relevance_reasoning TEXT,
    context_relevance_score DOUBLE PRECISION,
    context_relevance_reasoning TEXT,
    groundedness_score DOUBLE PRECISION,
    groundedness_reasoning TEXT,
    overall_score DOUBLE PRECISION,
    retrieved_chunks_count INTEGER NOT NULL DEFAULT 0,
    evaluated_chunks_count INTEGER NOT NULL DEFAULT 0,
    hallucination_detected BOOLEAN DEFAULT FALSE,
    missing_knowledge_detected BOOLEAN DEFAULT FALSE,
    evaluation_model VARCHAR(100),
    evaluation_time_ms BIGINT NOT NULL DEFAULT 0,
    evaluation_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    evaluated_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_remi_metrics_org_id ON remi_metrics(organization_id);
CREATE INDEX IF NOT EXISTS idx_remi_metrics_kb_id ON remi_metrics(knowledge_base_id);
CREATE INDEX IF NOT EXISTS idx_remi_metrics_query_id ON remi_metrics(query_id);
CREATE INDEX IF NOT EXISTS idx_remi_metrics_retrieval_id ON remi_metrics(retrieval_metrics_id);
CREATE INDEX IF NOT EXISTS idx_remi_metrics_evaluated_at ON remi_metrics(evaluated_at);
CREATE INDEX IF NOT EXISTS idx_remi_metrics_status ON remi_metrics(evaluation_status);
CREATE INDEX IF NOT EXISTS idx_remi_metrics_kb_evaluated ON remi_metrics(knowledge_base_id, evaluated_at DESC);
CREATE INDEX IF NOT EXISTS idx_remi_metrics_hallucination ON remi_metrics(knowledge_base_id, hallucination_detected) WHERE hallucination_detected = TRUE;
CREATE INDEX IF NOT EXISTS idx_remi_metrics_missing_knowledge ON remi_metrics(knowledge_base_id, missing_knowledge_detected) WHERE missing_knowledge_detected = TRUE;
