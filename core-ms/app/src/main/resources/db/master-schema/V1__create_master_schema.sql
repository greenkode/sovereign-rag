-- Master database schema for tenant management
-- This should be run on the sovereignrag_master database

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
COMMENT ON TABLE tenants IS 'Registry of all tenants (WordPress sites) using Sovereign RAG';

-- Reset search path to include both master and public
SET search_path TO master, public;
