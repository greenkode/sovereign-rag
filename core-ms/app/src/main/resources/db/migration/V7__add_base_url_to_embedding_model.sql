ALTER TABLE embedding_model ADD COLUMN IF NOT EXISTS base_url VARCHAR(500);

UPDATE embedding_model SET base_url = 'https://api.openai.com/v1' WHERE provider = 'openai' AND base_url IS NULL;
UPDATE embedding_model SET base_url = 'https://api.cohere.ai/v1' WHERE provider = 'cohere' AND base_url IS NULL;
