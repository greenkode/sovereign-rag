-- ============================================
-- V5: Add Admin Email and API Key Reset Tokens
-- ============================================
-- Purpose: Enable secure email-based API key reset flow
-- Author: Security Enhancement
-- Date: 2025-10-31

-- Add admin_email column to tenants table
-- This stores the email address for API key reset notifications
ALTER TABLE master.tenants
ADD COLUMN IF NOT EXISTS admin_email VARCHAR(255);

-- Create index for faster lookups
CREATE INDEX IF NOT EXISTS idx_tenants_admin_email ON master.tenants(admin_email);

-- Create reset_tokens table for API key reset verification
-- Tokens are valid for 15 minutes and single-use only
CREATE TABLE IF NOT EXISTS master.reset_tokens (
    id VARCHAR(255) PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45), -- IPv4 or IPv6

    -- Foreign key to tenants
    CONSTRAINT fk_reset_token_tenant
        FOREIGN KEY (tenant_id)
        REFERENCES master.tenants(id)
        ON DELETE CASCADE
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_reset_tokens_tenant_id ON master.reset_tokens(tenant_id);
CREATE INDEX IF NOT EXISTS idx_reset_tokens_token_hash ON master.reset_tokens(token_hash);
CREATE INDEX IF NOT EXISTS idx_reset_tokens_expires_at ON master.reset_tokens(expires_at);

-- Add comment explaining the schema
COMMENT ON TABLE master.reset_tokens IS 'Temporary tokens for secure API key reset flow. Tokens expire after 15 minutes and are single-use only.';
COMMENT ON COLUMN master.reset_tokens.token_hash IS 'BCrypt hash of the reset token for security';
COMMENT ON COLUMN master.reset_tokens.expires_at IS 'Token expiration timestamp (15 minutes from creation)';
COMMENT ON COLUMN master.reset_tokens.used_at IS 'Timestamp when token was used (null = not yet used)';
COMMENT ON COLUMN master.reset_tokens.ip_address IS 'IP address that requested the reset for security auditing';
