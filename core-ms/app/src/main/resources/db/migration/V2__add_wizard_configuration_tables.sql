CREATE TABLE IF NOT EXISTS region (
    code VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    continent VARCHAR(50) NOT NULL,
    city VARCHAR(100) NOT NULL,
    country VARCHAR(100) NOT NULL,
    country_code VARCHAR(10) NOT NULL,
    flag VARCHAR(10) NOT NULL,
    provider VARCHAR(50) NOT NULL DEFAULT 'default',
    enabled BOOLEAN NOT NULL DEFAULT true,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS language (
    code VARCHAR(10) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    native_name VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS embedding_model (
    id VARCHAR(100) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    model_id VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    provider VARCHAR(50) NOT NULL,
    dimensions INT NOT NULL,
    max_tokens INT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS embedding_model_language (
    model_id VARCHAR(100) NOT NULL REFERENCES embedding_model(id) ON DELETE CASCADE,
    language_code VARCHAR(10) NOT NULL,
    PRIMARY KEY (model_id, language_code)
);

CREATE TABLE IF NOT EXISTS embedding_model_optimized_language (
    model_id VARCHAR(100) NOT NULL REFERENCES embedding_model(id) ON DELETE CASCADE,
    language_code VARCHAR(10) NOT NULL,
    PRIMARY KEY (model_id, language_code)
);

INSERT INTO region (code, name, continent, city, country, country_code, flag, sort_order) VALUES
    ('eu-west-1', 'Europe (Frankfurt)', 'eu', 'Frankfurt', 'Germany', 'DE', '🇩🇪', 1),
    ('eu-west-2', 'Europe (Amsterdam)', 'eu', 'Amsterdam', 'Netherlands', 'NL', '🇳🇱', 2),
    ('eu-west-3', 'Europe (Paris)', 'eu', 'Paris', 'France', 'FR', '🇫🇷', 3),
    ('us-east-1', 'US East (Virginia)', 'us', 'Virginia', 'United States', 'US', '🇺🇸', 4),
    ('us-west-1', 'US West (California)', 'us', 'California', 'United States', 'US', '🇺🇸', 5),
    ('ap-southeast-1', 'Asia Pacific (Singapore)', 'apac', 'Singapore', 'Singapore', 'SG', '🇸🇬', 6),
    ('ap-northeast-1', 'Asia Pacific (Tokyo)', 'apac', 'Tokyo', 'Japan', 'JP', '🇯🇵', 7),
    ('ap-east-1', 'Asia Pacific (Hong Kong)', 'apac', 'Hong Kong', 'China', 'HK', '🇭🇰', 8)
ON CONFLICT (code) DO NOTHING;

INSERT INTO language (code, name, native_name, sort_order) VALUES
    ('en', 'English', 'English', 1),
    ('nl', 'Dutch', 'Nederlands', 2),
    ('de', 'German', 'Deutsch', 3),
    ('fr', 'French', 'Français', 4),
    ('es', 'Spanish', 'Español', 5),
    ('it', 'Italian', 'Italiano', 6),
    ('pt', 'Portuguese', 'Português', 7),
    ('ar', 'Arabic', 'العربية', 8),
    ('zh', 'Chinese', '中文', 9),
    ('ja', 'Japanese', '日本語', 10),
    ('ko', 'Korean', '한국어', 11),
    ('ru', 'Russian', 'Русский', 12),
    ('pl', 'Polish', 'Polski', 13),
    ('tr', 'Turkish', 'Türkçe', 14),
    ('vi', 'Vietnamese', 'Tiếng Việt', 15),
    ('th', 'Thai', 'ไทย', 16),
    ('hi', 'Hindi', 'हिन्दी', 17),
    ('other', 'Other', 'Other', 99)
ON CONFLICT (code) DO NOTHING;

INSERT INTO embedding_model (id, name, model_id, description, provider, dimensions, max_tokens, sort_order) VALUES
    ('openai-text-3-small', 'OpenAI Text Embedding 3 Small', 'text-embedding-3-small', 'Fast and cost-effective model optimized for English. Best for English-only content with good performance.', 'openai', 1536, 8191, 1),
    ('openai-text-3-large', 'OpenAI Text Embedding 3 Large', 'text-embedding-3-large', 'High-performance model with excellent accuracy. Best for demanding applications requiring top accuracy.', 'openai', 3072, 8191, 2),
    ('multilingual-e5-large', 'Multilingual E5 Large', 'intfloat/multilingual-e5-large', 'Excellent multilingual model supporting 100+ languages. Great for European and global content.', 'huggingface', 1024, 512, 3),
    ('multilingual-e5-base', 'Multilingual E5 Base', 'intfloat/multilingual-e5-base', 'Balanced multilingual model with good performance across many languages.', 'huggingface', 768, 512, 4),
    ('arabic-e5-base', 'Arabic E5 Base', 'intfloat/multilingual-e5-base-arabic', 'Specialized model optimized for Arabic content with English support.', 'huggingface', 768, 512, 5),
    ('chinese-text-embedding', 'Chinese Text Embedding', 'BAAI/bge-large-zh-v1.5', 'Specialized model optimized for Simplified and Traditional Chinese.', 'huggingface', 1024, 512, 6),
    ('japanese-e5-base', 'Japanese E5 Base', 'intfloat/multilingual-e5-base', 'Model with strong Japanese language support.', 'huggingface', 768, 512, 7),
    ('cohere-multilingual', 'Cohere Multilingual', 'embed-multilingual-v3.0', 'Enterprise-grade multilingual embeddings supporting 100+ languages.', 'cohere', 1024, 512, 8)
ON CONFLICT (id) DO NOTHING;

INSERT INTO embedding_model_language (model_id, language_code) VALUES
    ('openai-text-3-small', 'en'),
    ('openai-text-3-small', 'nl'),
    ('openai-text-3-small', 'de'),
    ('openai-text-3-small', 'fr'),
    ('openai-text-3-small', 'es'),
    ('openai-text-3-large', 'en'),
    ('openai-text-3-large', 'nl'),
    ('openai-text-3-large', 'de'),
    ('openai-text-3-large', 'fr'),
    ('openai-text-3-large', 'es'),
    ('openai-text-3-large', 'it'),
    ('openai-text-3-large', 'pt'),
    ('openai-text-3-large', 'zh'),
    ('openai-text-3-large', 'ja'),
    ('openai-text-3-large', 'ko'),
    ('multilingual-e5-large', 'en'),
    ('multilingual-e5-large', 'nl'),
    ('multilingual-e5-large', 'de'),
    ('multilingual-e5-large', 'fr'),
    ('multilingual-e5-large', 'es'),
    ('multilingual-e5-large', 'it'),
    ('multilingual-e5-large', 'pt'),
    ('multilingual-e5-large', 'ar'),
    ('multilingual-e5-large', 'zh'),
    ('multilingual-e5-large', 'ja'),
    ('multilingual-e5-large', 'ko'),
    ('multilingual-e5-large', 'ru'),
    ('multilingual-e5-large', 'pl'),
    ('multilingual-e5-large', 'tr'),
    ('multilingual-e5-large', 'vi'),
    ('multilingual-e5-large', 'th'),
    ('multilingual-e5-large', 'hi'),
    ('multilingual-e5-large', 'other'),
    ('multilingual-e5-base', 'en'),
    ('multilingual-e5-base', 'nl'),
    ('multilingual-e5-base', 'de'),
    ('multilingual-e5-base', 'fr'),
    ('multilingual-e5-base', 'es'),
    ('multilingual-e5-base', 'it'),
    ('multilingual-e5-base', 'pt'),
    ('multilingual-e5-base', 'ar'),
    ('multilingual-e5-base', 'zh'),
    ('multilingual-e5-base', 'ja'),
    ('multilingual-e5-base', 'ko'),
    ('multilingual-e5-base', 'other'),
    ('arabic-e5-base', 'ar'),
    ('arabic-e5-base', 'en'),
    ('chinese-text-embedding', 'zh'),
    ('chinese-text-embedding', 'en'),
    ('japanese-e5-base', 'ja'),
    ('japanese-e5-base', 'en'),
    ('cohere-multilingual', 'en'),
    ('cohere-multilingual', 'nl'),
    ('cohere-multilingual', 'de'),
    ('cohere-multilingual', 'fr'),
    ('cohere-multilingual', 'es'),
    ('cohere-multilingual', 'it'),
    ('cohere-multilingual', 'pt'),
    ('cohere-multilingual', 'ar'),
    ('cohere-multilingual', 'zh'),
    ('cohere-multilingual', 'ja'),
    ('cohere-multilingual', 'ko'),
    ('cohere-multilingual', 'ru'),
    ('cohere-multilingual', 'other')
ON CONFLICT (model_id, language_code) DO NOTHING;

INSERT INTO embedding_model_optimized_language (model_id, language_code) VALUES
    ('openai-text-3-small', 'en'),
    ('openai-text-3-large', 'en'),
    ('multilingual-e5-large', 'en'),
    ('multilingual-e5-large', 'nl'),
    ('multilingual-e5-large', 'de'),
    ('multilingual-e5-large', 'fr'),
    ('multilingual-e5-large', 'es'),
    ('multilingual-e5-base', 'en'),
    ('multilingual-e5-base', 'de'),
    ('multilingual-e5-base', 'fr'),
    ('arabic-e5-base', 'ar'),
    ('chinese-text-embedding', 'zh'),
    ('japanese-e5-base', 'ja'),
    ('cohere-multilingual', 'en'),
    ('cohere-multilingual', 'de'),
    ('cohere-multilingual', 'fr'),
    ('cohere-multilingual', 'es')
ON CONFLICT (model_id, language_code) DO NOTHING;

CREATE INDEX IF NOT EXISTS idx_region_enabled ON region(enabled);
CREATE INDEX IF NOT EXISTS idx_region_continent ON region(continent);
CREATE INDEX IF NOT EXISTS idx_language_enabled ON language(enabled);
CREATE INDEX IF NOT EXISTS idx_embedding_model_enabled ON embedding_model(enabled);
CREATE INDEX IF NOT EXISTS idx_embedding_model_language_lang ON embedding_model_language(language_code);
CREATE INDEX IF NOT EXISTS idx_embedding_model_optimized_lang ON embedding_model_optimized_language(language_code);
