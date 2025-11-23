-- Add tenant management tables to identity schema

CREATE TABLE IF NOT EXISTS identity.tenants (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    database_name VARCHAR(255) NOT NULL UNIQUE,
    api_key_hash VARCHAR(512) NOT NULL,
    status VARCHAR(50) NOT NULL,
    max_documents INTEGER NOT NULL DEFAULT 10000,
    max_embeddings INTEGER NOT NULL DEFAULT 50000,
    max_requests_per_day INTEGER NOT NULL DEFAULT 10000,
    subscription_tier VARCHAR(50) NOT NULL DEFAULT 'free',
    contact_email VARCHAR(500),
    contact_name VARCHAR(500),
    admin_email VARCHAR(255),
    wordpress_url VARCHAR(1000),
    wordpress_version VARCHAR(50),
    plugin_version VARCHAR(50),
    features JSONB DEFAULT '{}',
    settings JSONB DEFAULT '{}',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    last_active_at TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS identity.reset_tokens (
    id VARCHAR(255) PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    ip_address VARCHAR(45),
    FOREIGN KEY (tenant_id) REFERENCES identity.tenants(id)
);

CREATE INDEX IF NOT EXISTS idx_tenants_database_name ON identity.tenants(database_name);
CREATE INDEX IF NOT EXISTS idx_tenants_status ON identity.tenants(status);
CREATE INDEX IF NOT EXISTS idx_tenants_deleted_at ON identity.tenants(deleted_at);
CREATE INDEX IF NOT EXISTS idx_reset_tokens_tenant_id ON identity.reset_tokens(tenant_id);
CREATE INDEX IF NOT EXISTS idx_reset_tokens_expires_at ON identity.reset_tokens(expires_at);
