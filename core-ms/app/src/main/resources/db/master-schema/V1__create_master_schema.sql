CREATE SCHEMA IF NOT EXISTS master;

SET search_path TO master;

CREATE TABLE IF NOT EXISTS knowledge_bases (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(500) NOT NULL,
    description TEXT,
    organization_id UUID NOT NULL,
    schema_name VARCHAR(255) NOT NULL UNIQUE,
    oauth_client_id VARCHAR(100),
    api_key_hash VARCHAR(512),
    status VARCHAR(50) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DELETED')),
    max_documents INT DEFAULT 10000,
    max_embeddings INT DEFAULT 50000,
    max_requests_per_day INT DEFAULT 10000,
    subscription_tier VARCHAR(50) DEFAULT 'free',
    contact_email VARCHAR(500),
    contact_name VARCHAR(500),
    admin_email VARCHAR(255),
    wordpress_url TEXT,
    wordpress_version VARCHAR(50),
    plugin_version VARCHAR(50),
    features JSONB DEFAULT '{}'::jsonb,
    settings JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMP DEFAULT NOW(),
    last_modified_at TIMESTAMP DEFAULT NOW(),
    last_active_at TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_knowledge_bases_api_key ON knowledge_bases(api_key_hash);
CREATE INDEX IF NOT EXISTS idx_knowledge_bases_status ON knowledge_bases(status);
CREATE INDEX IF NOT EXISTS idx_knowledge_bases_schema_name ON knowledge_bases(schema_name);
CREATE INDEX IF NOT EXISTS idx_knowledge_bases_organization_id ON knowledge_bases(organization_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_bases_created_at ON knowledge_bases(created_at);
CREATE INDEX IF NOT EXISTS idx_knowledge_bases_admin_email ON knowledge_bases(admin_email);

CREATE OR REPLACE FUNCTION update_last_modified_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.last_modified_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_knowledge_bases_last_modified_at
    BEFORE UPDATE ON knowledge_bases
    FOR EACH ROW
    EXECUTE FUNCTION update_last_modified_at();

CREATE TABLE IF NOT EXISTS reset_tokens (
    id SERIAL PRIMARY KEY,
    knowledge_base_id VARCHAR(255) NOT NULL REFERENCES knowledge_bases(id),
    token VARCHAR(512) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_reset_tokens_token ON reset_tokens(token);
CREATE INDEX IF NOT EXISTS idx_reset_tokens_knowledge_base_id ON reset_tokens(knowledge_base_id);
CREATE INDEX IF NOT EXISTS idx_reset_tokens_expires_at ON reset_tokens(expires_at);

CREATE OR REPLACE FUNCTION update_billing_last_modified_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.last_modified_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE IF NOT EXISTS knowledge_base_subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    knowledge_base_id VARCHAR(255) NOT NULL REFERENCES knowledge_bases(id),
    stripe_customer_id VARCHAR(255),
    stripe_subscription_id VARCHAR(255),
    tier VARCHAR(50) NOT NULL DEFAULT 'FREE',
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    current_period_start TIMESTAMP,
    current_period_end TIMESTAMP,
    cancel_at_period_end BOOLEAN DEFAULT FALSE,
    canceled_at TIMESTAMP,
    trial_end TIMESTAMP,
    base_price_cents INT DEFAULT 0,
    currency VARCHAR(3) DEFAULT 'USD',
    included_tokens BIGINT DEFAULT 0,
    overage_rate_per_1k NUMERIC(10, 4) DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    last_modified_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(knowledge_base_id)
);

CREATE INDEX IF NOT EXISTS idx_knowledge_base_subscriptions_kb ON knowledge_base_subscriptions(knowledge_base_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_base_subscriptions_status ON knowledge_base_subscriptions(status);
CREATE INDEX IF NOT EXISTS idx_knowledge_base_subscriptions_stripe ON knowledge_base_subscriptions(stripe_subscription_id);

CREATE TRIGGER update_knowledge_base_subscriptions_last_modified_at
    BEFORE UPDATE ON knowledge_base_subscriptions
    FOR EACH ROW
    EXECUTE FUNCTION update_billing_last_modified_at();

CREATE TABLE IF NOT EXISTS token_usage (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    knowledge_base_id VARCHAR(255) NOT NULL REFERENCES knowledge_bases(id),
    request_id VARCHAR(255),
    operation_type VARCHAR(50) NOT NULL,
    model VARCHAR(100),
    input_tokens INT DEFAULT 0,
    output_tokens INT DEFAULT 0,
    total_tokens INT DEFAULT 0,
    embedding_tokens INT DEFAULT 0,
    cost_cents NUMERIC(10, 4) DEFAULT 0,
    reported_to_stripe BOOLEAN DEFAULT FALSE,
    stripe_usage_record_id VARCHAR(255),
    metadata JSONB,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_token_usage_kb_date ON token_usage(knowledge_base_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_token_usage_billing_period ON token_usage(knowledge_base_id, created_at) WHERE reported_to_stripe = false;

CREATE TABLE IF NOT EXISTS invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    knowledge_base_id VARCHAR(255) NOT NULL REFERENCES knowledge_bases(id),
    stripe_invoice_id VARCHAR(255) UNIQUE,
    number VARCHAR(100),
    status VARCHAR(50),
    currency VARCHAR(3) DEFAULT 'USD',
    subtotal_cents INT DEFAULT 0,
    tax_cents INT DEFAULT 0,
    total_cents INT DEFAULT 0,
    amount_due_cents INT DEFAULT 0,
    amount_paid_cents INT DEFAULT 0,
    period_start TIMESTAMP,
    period_end TIMESTAMP,
    due_date TIMESTAMP,
    paid_at TIMESTAMP,
    invoice_pdf_url TEXT,
    hosted_invoice_url TEXT,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT NOW(),
    last_modified_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_invoices_kb ON invoices(knowledge_base_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_invoices_stripe ON invoices(stripe_invoice_id);
CREATE INDEX IF NOT EXISTS idx_invoices_status ON invoices(status);

CREATE TABLE IF NOT EXISTS payment_methods (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    knowledge_base_id VARCHAR(255) NOT NULL REFERENCES knowledge_bases(id),
    stripe_payment_method_id VARCHAR(255) NOT NULL UNIQUE,
    type VARCHAR(50),
    card_brand VARCHAR(50),
    card_last4 VARCHAR(4),
    card_exp_month INT,
    card_exp_year INT,
    is_default BOOLEAN DEFAULT FALSE,
    billing_name VARCHAR(255),
    billing_email VARCHAR(255),
    billing_address JSONB,
    created_at TIMESTAMP DEFAULT NOW(),
    last_modified_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_payment_methods_kb ON payment_methods(knowledge_base_id);
CREATE INDEX IF NOT EXISTS idx_payment_methods_default ON payment_methods(knowledge_base_id, is_default) WHERE is_default = true;

CREATE TABLE IF NOT EXISTS billing_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    knowledge_base_id VARCHAR(255) REFERENCES knowledge_bases(id),
    stripe_event_id VARCHAR(255) UNIQUE NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    processed BOOLEAN DEFAULT FALSE,
    processing_error TEXT,
    payload JSONB,
    created_at TIMESTAMP DEFAULT NOW(),
    processed_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_billing_events_stripe ON billing_events(stripe_event_id);
CREATE INDEX IF NOT EXISTS idx_billing_events_kb ON billing_events(knowledge_base_id);
CREATE INDEX IF NOT EXISTS idx_billing_events_type ON billing_events(event_type);
CREATE INDEX IF NOT EXISTS idx_billing_events_unprocessed ON billing_events(created_at) WHERE processed = false;

SET search_path TO master, public;
