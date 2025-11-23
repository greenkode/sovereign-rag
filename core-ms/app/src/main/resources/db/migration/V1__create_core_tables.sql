-- Create core schema for business domain objects
CREATE SCHEMA IF NOT EXISTS core;

-- Escalations table (customer support escalations)
CREATE TABLE IF NOT EXISTS core.escalations (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL,
    reason TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'pending',
    priority VARCHAR(20) NOT NULL DEFAULT 'normal',
    user_email VARCHAR(500) NOT NULL,
    user_name VARCHAR(500),
    user_phone VARCHAR(100),
    assigned_to VARCHAR(255),
    assigned_at TIMESTAMP,
    resolved_at TIMESTAMP,
    resolution_notes TEXT,
    user_message TEXT,
    language VARCHAR(10),
    persona VARCHAR(100) NOT NULL,
    email_sent BOOLEAN NOT NULL DEFAULT FALSE,
    reviewed BOOLEAN NOT NULL DEFAULT FALSE,
    reviewed_at TIMESTAMP,
    reviewed_by VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    metadata JSONB DEFAULT '{}'
);

-- Unanswered queries table (knowledge gap tracking)
CREATE TABLE IF NOT EXISTS core.unanswered_queries (
    id UUID PRIMARY KEY,
    query TEXT NOT NULL,
    response TEXT,
    language VARCHAR(255),
    session_id UUID,
    confidence_score DOUBLE PRECISION,
    reason VARCHAR(255),
    status VARCHAR(255) NOT NULL DEFAULT 'open',
    used_general_knowledge BOOLEAN NOT NULL DEFAULT FALSE,
    resolved_at TIMESTAMP,
    resolution_notes TEXT,
    created_at TIMESTAMP NOT NULL,
    occurrence_count INTEGER NOT NULL DEFAULT 1,
    last_occurred_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

-- Pricing table (pricing rules and configurations)
CREATE TABLE IF NOT EXISTS core.pricing (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL,
    account_type VARCHAR(50) NOT NULL,
    account_public_id UUID,
    transaction_type VARCHAR(50) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    valid_from TIMESTAMP NOT NULL,
    valid_until TIMESTAMP,
    product_id UUID,
    integrator_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Pricing data table (pricing calculation details)
CREATE TABLE IF NOT EXISTS core.pricing_data (
    id BIGSERIAL PRIMARY KEY,
    pricing_type VARCHAR(50) NOT NULL,
    calculation VARCHAR(50) NOT NULL,
    value DECIMAL(19,4) NOT NULL,
    expression TEXT,
    pricing_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY (pricing_id) REFERENCES core.pricing(id)
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_escalations_session_id ON core.escalations(session_id);
CREATE INDEX IF NOT EXISTS idx_escalations_status ON core.escalations(status);
CREATE INDEX IF NOT EXISTS idx_escalations_created_at ON core.escalations(created_at);

CREATE INDEX IF NOT EXISTS idx_unanswered_queries_status ON core.unanswered_queries(status);
CREATE INDEX IF NOT EXISTS idx_unanswered_queries_session_id ON core.unanswered_queries(session_id);
CREATE INDEX IF NOT EXISTS idx_unanswered_queries_created_at ON core.unanswered_queries(created_at);

CREATE INDEX IF NOT EXISTS idx_pricing_public_id ON core.pricing(public_id);
CREATE INDEX IF NOT EXISTS idx_pricing_account_type ON core.pricing(account_type);
CREATE INDEX IF NOT EXISTS idx_pricing_transaction_type ON core.pricing(transaction_type);
CREATE INDEX IF NOT EXISTS idx_pricing_valid_from ON core.pricing(valid_from);

CREATE INDEX IF NOT EXISTS idx_pricing_data_pricing_id ON core.pricing_data(pricing_id);
