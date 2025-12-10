CREATE TABLE IF NOT EXISTS quality_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    knowledge_base_id UUID,
    knowledge_source_id UUID,
    ingestion_job_id UUID,
    metric_type VARCHAR(50) NOT NULL,
    metric_source VARCHAR(50) NOT NULL,
    overall_score DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    coherence_score DOUBLE PRECISION,
    boundary_score DOUBLE PRECISION,
    size_distribution_score DOUBLE PRECISION,
    context_sufficiency_score DOUBLE PRECISION,
    information_preservation_score DOUBLE PRECISION,
    chunk_count INTEGER NOT NULL DEFAULT 0,
    average_chunk_size DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    min_chunk_size INTEGER,
    max_chunk_size INTEGER,
    chunking_strategy VARCHAR(100),
    embedding_model VARCHAR(100),
    processing_time_ms BIGINT NOT NULL DEFAULT 0,
    evaluated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_quality_metrics_org_id ON quality_metrics(organization_id);
CREATE INDEX IF NOT EXISTS idx_quality_metrics_kb_id ON quality_metrics(knowledge_base_id);
CREATE INDEX IF NOT EXISTS idx_quality_metrics_ks_id ON quality_metrics(knowledge_source_id);
CREATE INDEX IF NOT EXISTS idx_quality_metrics_job_id ON quality_metrics(ingestion_job_id);
CREATE INDEX IF NOT EXISTS idx_quality_metrics_evaluated_at ON quality_metrics(evaluated_at);
CREATE INDEX IF NOT EXISTS idx_quality_metrics_type_source ON quality_metrics(metric_type, metric_source);
CREATE INDEX IF NOT EXISTS idx_quality_metrics_kb_evaluated ON quality_metrics(knowledge_base_id, evaluated_at DESC);

CREATE TABLE IF NOT EXISTS retrieval_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    knowledge_base_id UUID,
    query_id UUID,
    query_text TEXT,
    query_embedding_time_ms BIGINT NOT NULL DEFAULT 0,
    search_time_ms BIGINT NOT NULL DEFAULT 0,
    total_time_ms BIGINT NOT NULL DEFAULT 0,
    results_returned INTEGER NOT NULL DEFAULT 0,
    results_requested INTEGER NOT NULL DEFAULT 0,
    top_result_score DOUBLE PRECISION,
    average_result_score DOUBLE PRECISION,
    lowest_result_score DOUBLE PRECISION,
    score_variance DOUBLE PRECISION,
    distinct_sources_count INTEGER NOT NULL DEFAULT 0,
    user_feedback_score DOUBLE PRECISION,
    clicked_result_index INTEGER,
    result_relevance_ratings TEXT,
    embedding_model VARCHAR(100),
    search_strategy VARCHAR(100),
    queried_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_retrieval_metrics_org_id ON retrieval_metrics(organization_id);
CREATE INDEX IF NOT EXISTS idx_retrieval_metrics_kb_id ON retrieval_metrics(knowledge_base_id);
CREATE INDEX IF NOT EXISTS idx_retrieval_metrics_query_id ON retrieval_metrics(query_id);
CREATE INDEX IF NOT EXISTS idx_retrieval_metrics_queried_at ON retrieval_metrics(queried_at);
CREATE INDEX IF NOT EXISTS idx_retrieval_metrics_kb_queried ON retrieval_metrics(knowledge_base_id, queried_at DESC);
CREATE INDEX IF NOT EXISTS idx_retrieval_metrics_feedback ON retrieval_metrics(knowledge_base_id, user_feedback_score) WHERE user_feedback_score IS NOT NULL;
