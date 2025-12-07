ALTER TABLE knowledge_base ADD COLUMN IF NOT EXISTS region_code VARCHAR(20) NOT NULL DEFAULT 'eu-west';
ALTER TABLE knowledge_base ADD COLUMN IF NOT EXISTS embedding_model_id VARCHAR(100);
ALTER TABLE knowledge_base ADD COLUMN IF NOT EXISTS requires_encryption BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_knowledge_base_region_code ON knowledge_base(region_code);
