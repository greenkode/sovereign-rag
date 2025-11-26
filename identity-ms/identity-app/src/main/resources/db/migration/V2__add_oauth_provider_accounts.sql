CREATE TABLE IF NOT EXISTS identity.oauth_provider_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    provider VARCHAR(50) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    provider_email VARCHAR(255),
    linked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP,
    created_by VARCHAR(255),
    last_modified_at TIMESTAMP,
    last_modified_by VARCHAR(255),
    CONSTRAINT fk_oauth_provider_user FOREIGN KEY (user_id) REFERENCES identity.oauth_users(id) ON DELETE CASCADE,
    CONSTRAINT uk_provider_user UNIQUE (provider, provider_user_id)
);

CREATE INDEX IF NOT EXISTS idx_oauth_provider_user_id ON identity.oauth_provider_accounts(user_id);
CREATE INDEX IF NOT EXISTS idx_oauth_provider_email ON identity.oauth_provider_accounts(provider_email);
CREATE INDEX IF NOT EXISTS idx_oauth_provider ON identity.oauth_provider_accounts(provider, provider_user_id);
