-- Add version column for optimistic locking support
ALTER TABLE unanswered_queries
ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- Add comment to explain the column
COMMENT ON COLUMN unanswered_queries.version IS 'Version number for optimistic locking';
