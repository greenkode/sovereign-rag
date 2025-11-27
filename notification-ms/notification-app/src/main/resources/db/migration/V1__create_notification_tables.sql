CREATE TABLE IF NOT EXISTS message_templates (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    channel VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    external_id VARCHAR(255),
    locale VARCHAR(10) NOT NULL DEFAULT 'en',
    recipient_type VARCHAR(50) NOT NULL DEFAULT 'INDIVIDUAL',
    active BOOLEAN NOT NULL DEFAULT true,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_at TIMESTAMP,
    last_modified_by VARCHAR(255),
    CONSTRAINT uk_message_template_name_channel_locale_type UNIQUE (name, channel, locale, recipient_type)
);

CREATE INDEX IF NOT EXISTS idx_message_templates_name ON message_templates(name);
CREATE INDEX IF NOT EXISTS idx_message_templates_active ON message_templates(active);

CREATE TABLE IF NOT EXISTS messages (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL UNIQUE,
    channel VARCHAR(50) NOT NULL,
    template_id BIGINT REFERENCES message_templates(id),
    template_name VARCHAR(100) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    recipient_name VARCHAR(255),
    delivery_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    request TEXT,
    response TEXT,
    priority VARCHAR(50) NOT NULL DEFAULT 'NORMAL',
    client_identifier VARCHAR(255) NOT NULL,
    locale VARCHAR(10) NOT NULL DEFAULT 'en',
    delivery_date TIMESTAMP,
    sent_message_id VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_at TIMESTAMP,
    last_modified_by VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_messages_public_id ON messages(public_id);
CREATE INDEX IF NOT EXISTS idx_messages_client_identifier ON messages(client_identifier);
CREATE INDEX IF NOT EXISTS idx_messages_delivery_status ON messages(delivery_status);

INSERT INTO message_templates (name, channel, title, content, external_id, locale, recipient_type, active, created_at, created_by, last_modified_at, last_modified_by)
VALUES
    ('EMAIL_VERIFICATION', 'EMAIL', 'Verify Your Email', '', 'd-2adfb4c73d3c47c3b86da213c56c65c0', 'en', 'INDIVIDUAL', true, NOW(), 'system', NOW(), 'system'),
    ('TWO_FACTOR_AUTH', 'EMAIL', 'Your Verification Code', '', 'd-a071ac7c7277418b81bd24ff059c3e46', 'en', 'INDIVIDUAL', true, NOW(), 'system', NOW(), 'system'),
    ('PASSWORD_RESET', 'EMAIL', 'Reset Your Password', '', 'd-e04d84d376684a6e985eaac7b58b655e', 'en', 'INDIVIDUAL', true, NOW(), 'system', NOW(), 'system'),
    ('WELCOME', 'EMAIL', 'Welcome to Sovereign RAG', '', 'd-20e8684839d64278855b05594d5fe674', 'en', 'INDIVIDUAL', true, NOW(), 'system', NOW(), 'system'),
    ('MERCHANT_USER_INVITATION', 'EMAIL', 'You''ve Been Invited', '', 'd-dca4da5a07304bd5945578aa9d53abef ', 'en', 'INDIVIDUAL', true, NOW(), 'system', NOW(), 'system')
ON CONFLICT (name, channel, locale, recipient_type) DO NOTHING;
