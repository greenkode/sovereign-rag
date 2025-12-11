CREATE TABLE IF NOT EXISTS llm_model (
    id VARCHAR(100) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    model_id VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    provider VARCHAR(50) NOT NULL,
    provider_type VARCHAR(50) NOT NULL,
    max_tokens INT NOT NULL,
    context_window INT NOT NULL,
    supports_streaming BOOLEAN NOT NULL DEFAULT true,
    supports_function_calling BOOLEAN NOT NULL DEFAULT false,
    privacy_level VARCHAR(50) NOT NULL DEFAULT 'STANDARD',
    min_tier VARCHAR(50) NOT NULL DEFAULT 'TRIAL',
    cost_per_1k_input_tokens DECIMAL(10, 6),
    cost_per_1k_output_tokens DECIMAL(10, 6),
    base_url VARCHAR(500),
    enabled BOOLEAN NOT NULL DEFAULT true,
    is_default BOOLEAN NOT NULL DEFAULT false,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS llm_model_capability (
    model_id VARCHAR(100) NOT NULL REFERENCES llm_model(id) ON DELETE CASCADE,
    capability VARCHAR(100) NOT NULL,
    PRIMARY KEY (model_id, capability)
);

ALTER TABLE knowledge_base ADD COLUMN IF NOT EXISTS llm_model_id VARCHAR(100) REFERENCES llm_model(id);

INSERT INTO llm_model (id, name, model_id, description, provider, provider_type, max_tokens, context_window, supports_streaming, supports_function_calling, privacy_level, min_tier, is_default, sort_order) VALUES
    ('ollama-llama3.2-3b', 'Llama 3.2 (3B)', 'llama3.2:3b', 'Fast, privacy-focused local model. Best for general-purpose tasks with GDPR compliance.', 'ollama', 'LOCAL', 4096, 128000, true, false, 'MAXIMUM', 'TRIAL', true, 1),
    ('ollama-llama3.2-1b', 'Llama 3.2 (1B)', 'llama3.2:1b', 'Lightweight local model for quick responses. Great for simple Q&A.', 'ollama', 'LOCAL', 4096, 128000, true, false, 'MAXIMUM', 'TRIAL', false, 2),
    ('ollama-mistral-7b', 'Mistral 7B', 'mistral:7b', 'High-quality local model with excellent reasoning. Good balance of speed and quality.', 'ollama', 'LOCAL', 4096, 32000, true, false, 'MAXIMUM', 'TRIAL', false, 3),
    ('ollama-qwen2.5-7b', 'Qwen 2.5 (7B)', 'qwen2.5:7b', 'Multilingual local model with strong performance across languages.', 'ollama', 'LOCAL', 4096, 128000, true, false, 'MAXIMUM', 'TRIAL', false, 4),
    ('openai-gpt-4o-mini', 'GPT-4o Mini', 'gpt-4o-mini', 'Cost-effective OpenAI model with good performance. Best for high-volume applications.', 'openai', 'CLOUD', 16384, 128000, true, true, 'STANDARD', 'PROFESSIONAL', false, 5),
    ('openai-gpt-4o', 'GPT-4o', 'gpt-4o', 'OpenAI flagship model with excellent reasoning and instruction following.', 'openai', 'CLOUD', 16384, 128000, true, true, 'STANDARD', 'PROFESSIONAL', false, 6),
    ('openai-gpt-4-turbo', 'GPT-4 Turbo', 'gpt-4-turbo', 'Previous generation flagship with strong performance.', 'openai', 'CLOUD', 4096, 128000, true, true, 'STANDARD', 'ENTERPRISE', false, 7),
    ('anthropic-claude-3.5-sonnet', 'Claude 3.5 Sonnet', 'claude-3-5-sonnet-20241022', 'Anthropic balanced model with excellent coding and analysis capabilities.', 'anthropic', 'CLOUD', 8192, 200000, true, true, 'STANDARD', 'PROFESSIONAL', false, 8),
    ('anthropic-claude-3.5-haiku', 'Claude 3.5 Haiku', 'claude-3-5-haiku-20241022', 'Fast Anthropic model for quick responses. Good for high-volume use cases.', 'anthropic', 'CLOUD', 8192, 200000, true, true, 'STANDARD', 'PROFESSIONAL', false, 9),
    ('anthropic-claude-3-opus', 'Claude 3 Opus', 'claude-3-opus-20240229', 'Anthropic most capable model for complex tasks.', 'anthropic', 'CLOUD', 4096, 200000, true, true, 'STANDARD', 'ENTERPRISE', false, 10)
ON CONFLICT (id) DO NOTHING;

INSERT INTO llm_model_capability (model_id, capability) VALUES
    ('ollama-llama3.2-3b', 'chat'),
    ('ollama-llama3.2-3b', 'summarization'),
    ('ollama-llama3.2-3b', 'rag'),
    ('ollama-llama3.2-1b', 'chat'),
    ('ollama-llama3.2-1b', 'rag'),
    ('ollama-mistral-7b', 'chat'),
    ('ollama-mistral-7b', 'summarization'),
    ('ollama-mistral-7b', 'rag'),
    ('ollama-mistral-7b', 'reasoning'),
    ('ollama-qwen2.5-7b', 'chat'),
    ('ollama-qwen2.5-7b', 'summarization'),
    ('ollama-qwen2.5-7b', 'rag'),
    ('ollama-qwen2.5-7b', 'multilingual'),
    ('openai-gpt-4o-mini', 'chat'),
    ('openai-gpt-4o-mini', 'summarization'),
    ('openai-gpt-4o-mini', 'rag'),
    ('openai-gpt-4o-mini', 'function_calling'),
    ('openai-gpt-4o', 'chat'),
    ('openai-gpt-4o', 'summarization'),
    ('openai-gpt-4o', 'rag'),
    ('openai-gpt-4o', 'reasoning'),
    ('openai-gpt-4o', 'function_calling'),
    ('openai-gpt-4o', 'vision'),
    ('openai-gpt-4-turbo', 'chat'),
    ('openai-gpt-4-turbo', 'summarization'),
    ('openai-gpt-4-turbo', 'rag'),
    ('openai-gpt-4-turbo', 'reasoning'),
    ('openai-gpt-4-turbo', 'function_calling'),
    ('anthropic-claude-3.5-sonnet', 'chat'),
    ('anthropic-claude-3.5-sonnet', 'summarization'),
    ('anthropic-claude-3.5-sonnet', 'rag'),
    ('anthropic-claude-3.5-sonnet', 'reasoning'),
    ('anthropic-claude-3.5-sonnet', 'coding'),
    ('anthropic-claude-3.5-sonnet', 'function_calling'),
    ('anthropic-claude-3.5-haiku', 'chat'),
    ('anthropic-claude-3.5-haiku', 'summarization'),
    ('anthropic-claude-3.5-haiku', 'rag'),
    ('anthropic-claude-3.5-haiku', 'function_calling'),
    ('anthropic-claude-3-opus', 'chat'),
    ('anthropic-claude-3-opus', 'summarization'),
    ('anthropic-claude-3-opus', 'rag'),
    ('anthropic-claude-3-opus', 'reasoning'),
    ('anthropic-claude-3-opus', 'coding'),
    ('anthropic-claude-3-opus', 'function_calling'),
    ('anthropic-claude-3-opus', 'analysis')
ON CONFLICT (model_id, capability) DO NOTHING;

CREATE INDEX IF NOT EXISTS idx_llm_model_enabled ON llm_model(enabled);
CREATE INDEX IF NOT EXISTS idx_llm_model_provider ON llm_model(provider);
CREATE INDEX IF NOT EXISTS idx_llm_model_provider_type ON llm_model(provider_type);
CREATE INDEX IF NOT EXISTS idx_llm_model_min_tier ON llm_model(min_tier);
CREATE INDEX IF NOT EXISTS idx_llm_model_privacy_level ON llm_model(privacy_level);
CREATE INDEX IF NOT EXISTS idx_llm_model_is_default ON llm_model(is_default) WHERE is_default = true;
CREATE INDEX IF NOT EXISTS idx_llm_model_capability_cap ON llm_model_capability(capability);
CREATE INDEX IF NOT EXISTS idx_knowledge_base_llm_model ON knowledge_base(llm_model_id);
