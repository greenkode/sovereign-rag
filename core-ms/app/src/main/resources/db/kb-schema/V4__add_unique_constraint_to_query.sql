-- Add unique constraint to query column for atomic upsert operations
-- This enables PostgreSQL's INSERT ... ON CONFLICT to work properly
ALTER TABLE unanswered_queries
ADD CONSTRAINT unanswered_queries_query_unique UNIQUE (query);

COMMENT ON CONSTRAINT unanswered_queries_query_unique ON unanswered_queries IS 'Ensures query text uniqueness for atomic upsert operations';
