-- Master database schema for tenant management
-- This should be run on the compilot_master database

-- Create master schema for logical organization
CREATE SCHEMA IF NOT EXISTS master;

-- Set search path to master schema
SET search_path TO master;

-- Tenant registry table
CREATE TABLE IF NOT EXISTS tenants (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(500) NOT NULL,
    database_name VARCHAR(255) NOT NULL UNIQUE,
    api_key_hash VARCHAR(512) NOT NULL,
    status VARCHAR(50) DEFAULT 'active' CHECK (status IN ('active', 'suspended', 'deleted')),

    -- Limits and quotas
    max_documents INT DEFAULT 10000,
    max_embeddings INT DEFAULT 50000,
    max_requests_per_day INT DEFAULT 10000,

    -- Billing info (for future)
    subscription_tier VARCHAR(50) DEFAULT 'free',

    -- Contact info
    contact_email VARCHAR(500),
    contact_name VARCHAR(500),

    -- WordPress site info
    wordpress_url TEXT,
    wordpress_version VARCHAR(50),
    plugin_version VARCHAR(50),

    -- Features and settings
    features JSONB DEFAULT '{}'::jsonb,
    settings JSONB DEFAULT '{}'::jsonb,

    -- Timestamps
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    last_active_at TIMESTAMP,

    -- Soft delete
    deleted_at TIMESTAMP
);

-- Indexes for tenants table
CREATE INDEX IF NOT EXISTS idx_tenants_api_key ON tenants(api_key_hash);
CREATE INDEX IF NOT EXISTS idx_tenants_status ON tenants(status);
CREATE INDEX IF NOT EXISTS idx_tenants_database_name ON tenants(database_name);
CREATE INDEX IF NOT EXISTS idx_tenants_created_at ON tenants(created_at);

-- Tenant usage tracking table
CREATE TABLE IF NOT EXISTS tenant_usage (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(255) REFERENCES tenants(id) ON DELETE CASCADE,
    date DATE NOT NULL,

    -- Counters
    api_requests INT DEFAULT 0,
    documents_ingested INT DEFAULT 0,
    search_queries INT DEFAULT 0,
    chat_messages INT DEFAULT 0,

    -- Resources
    storage_bytes BIGINT DEFAULT 0,

    -- Costs (for billing)
    embedding_tokens BIGINT DEFAULT 0,
    llm_tokens BIGINT DEFAULT 0,

    created_at TIMESTAMP DEFAULT NOW(),

    UNIQUE(tenant_id, date)
);

CREATE INDEX IF NOT EXISTS idx_tenant_usage_tenant_date ON tenant_usage(tenant_id, date);

-- API key management (support multiple keys per tenant)
CREATE TABLE IF NOT EXISTS api_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(255) REFERENCES tenants(id) ON DELETE CASCADE,
    key_hash VARCHAR(512) NOT NULL UNIQUE,
    name VARCHAR(255),

    -- Permissions (for future fine-grained access)
    permissions JSONB DEFAULT '["read", "write"]'::jsonb,

    -- Key metadata
    created_at TIMESTAMP DEFAULT NOW(),
    last_used_at TIMESTAMP,
    expires_at TIMESTAMP,
    revoked_at TIMESTAMP,

    -- Track usage per key
    usage_count BIGINT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_api_keys_tenant ON api_keys(tenant_id);
CREATE INDEX IF NOT EXISTS idx_api_keys_hash ON api_keys(key_hash);

-- Audit log table
CREATE TABLE IF NOT EXISTS audit_log (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(255),
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100),
    resource_id VARCHAR(255),
    user_agent TEXT,
    ip_address INET,
    details JSONB,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_log_tenant ON audit_log(tenant_id, created_at);
CREATE INDEX IF NOT EXISTS idx_audit_log_action ON audit_log(action);
CREATE INDEX IF NOT EXISTS idx_audit_log_created_at ON audit_log(created_at);

-- Update trigger function for updated_at columns
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply update trigger to tenants table
DROP TRIGGER IF EXISTS update_tenants_updated_at ON tenants;
CREATE TRIGGER update_tenants_updated_at
    BEFORE UPDATE ON tenants
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Comments for documentation
COMMENT ON TABLE tenants IS 'Registry of all tenants (WordPress sites) using Compilot AI';
COMMENT ON TABLE tenant_usage IS 'Daily usage metrics per tenant for billing and monitoring';
COMMENT ON TABLE api_keys IS 'API keys for tenant authentication (supports multiple keys per tenant)';
COMMENT ON TABLE audit_log IS 'Audit trail of all tenant actions for security and compliance';

-- Reset search path to include both master and public
SET search_path TO master, public;
