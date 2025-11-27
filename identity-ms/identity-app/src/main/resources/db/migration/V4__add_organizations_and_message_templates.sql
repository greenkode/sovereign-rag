ALTER TABLE identity.oauth_registered_clients ADD COLUMN IF NOT EXISTS domain VARCHAR(255);
ALTER TABLE identity.oauth_registered_clients ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'ACTIVE';
ALTER TABLE identity.oauth_registered_clients ADD COLUMN IF NOT EXISTS plan VARCHAR(50) DEFAULT 'FREE';

CREATE UNIQUE INDEX IF NOT EXISTS idx_oauth_registered_clients_domain ON identity.oauth_registered_clients(domain);
CREATE INDEX IF NOT EXISTS idx_oauth_registered_clients_status ON identity.oauth_registered_clients(status);

CREATE TABLE IF NOT EXISTS identity.message_templates (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    channel VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    external_id VARCHAR(255),
    locale VARCHAR(10) NOT NULL DEFAULT 'en',
    recipient_type VARCHAR(100) NOT NULL DEFAULT 'INDIVIDUAL',
    active BOOLEAN NOT NULL DEFAULT true,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_at TIMESTAMP,
    last_modified_by VARCHAR(255),
    CONSTRAINT uk_message_template_name_channel_locale UNIQUE (name, channel, locale)
);

CREATE INDEX IF NOT EXISTS idx_message_templates_name ON identity.message_templates(name);

INSERT INTO identity.message_templates (name, channel, title, content, external_id, locale, recipient_type, active, created_at, created_by, last_modified_at, last_modified_by)
VALUES
    ('EMAIL_VERIFICATION', 'EMAIL', 'Verify Your Email', '', NULL, 'en', 'INDIVIDUAL', true, NOW(), 'system', NOW(), 'system'),
    ('TWO_FACTOR_AUTH', 'EMAIL', 'Your Verification Code', '', NULL, 'en', 'INDIVIDUAL', true, NOW(), 'system', NOW(), 'system'),
    ('PASSWORD_RESET', 'EMAIL', 'Reset Your Password', '', NULL, 'en', 'INDIVIDUAL', true, NOW(), 'system', NOW(), 'system'),
    ('WELCOME', 'EMAIL', 'Welcome to Sovereign RAG', '', NULL, 'en', 'INDIVIDUAL', true, NOW(), 'system', NOW(), 'system')
ON CONFLICT (name, channel, locale) DO NOTHING;
