-- ============================================
-- V10: Billing and Subscription Management
-- ============================================
-- Purpose: Track subscriptions, payments, and token usage for billing
-- Provider: Stripe (can be adapted for Adyen)

SET search_path TO master;

-- ============================================
-- Subscription Plans
-- ============================================
CREATE TABLE subscription_plans (
    id VARCHAR(100) PRIMARY KEY,  -- e.g., 'starter', 'professional', 'enterprise'
    name VARCHAR(255) NOT NULL,
    description TEXT,

    -- Pricing
    base_price DECIMAL(10, 2) NOT NULL,  -- Monthly base price
    currency VARCHAR(3) DEFAULT 'USD',
    billing_period VARCHAR(20) DEFAULT 'monthly' CHECK (billing_period IN ('monthly', 'yearly')),

    -- Quotas
    included_tokens BIGINT DEFAULT 0,  -- Tokens included in base price
    max_knowledge_bases INT DEFAULT 1,
    max_users INT DEFAULT 5,
    max_documents INT DEFAULT 10000,

    -- Overage pricing (per 1000 tokens)
    overage_price_per_1k_tokens DECIMAL(10, 6),

    -- Features
    features JSONB DEFAULT '[]'::jsonb,  -- ["advanced_analytics", "priority_support"]

    -- Stripe/Adyen IDs
    stripe_price_id VARCHAR(255),  -- Stripe Price ID
    stripe_product_id VARCHAR(255),  -- Stripe Product ID

    -- Lifecycle
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_subscription_plans_active ON subscription_plans(active);

-- Seed default plans
INSERT INTO subscription_plans (id, name, description, base_price, currency, included_tokens, max_knowledge_bases, max_users, overage_price_per_1k_tokens, features) VALUES
('free', 'Free', 'Perfect for testing and small projects', 0.00, 'USD', 100000, 1, 1, NULL, '["basic_support"]'::jsonb),
('starter', 'Starter', 'For small teams getting started', 29.00, 'USD', 1000000, 3, 5, 0.015, '["email_support", "api_access"]'::jsonb),
('professional', 'Professional', 'For growing businesses', 99.00, 'USD', 5000000, 10, 20, 0.012, '["priority_support", "advanced_analytics", "sso"]'::jsonb),
('enterprise', 'Enterprise', 'For large organizations', 499.00, 'USD', 50000000, -1, -1, 0.008, '["dedicated_support", "sla", "custom_deployment", "audit_logs"]'::jsonb);

-- ============================================
-- Tenant Subscriptions
-- ============================================
CREATE TABLE tenant_subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(255) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    plan_id VARCHAR(100) NOT NULL REFERENCES subscription_plans(id),

    -- Stripe/Adyen references
    stripe_customer_id VARCHAR(255),  -- Stripe Customer ID
    stripe_subscription_id VARCHAR(255),  -- Stripe Subscription ID

    -- Subscription status
    status VARCHAR(50) DEFAULT 'active' CHECK (status IN ('active', 'trialing', 'past_due', 'canceled', 'unpaid')),

    -- Billing cycle
    current_period_start TIMESTAMP NOT NULL,
    current_period_end TIMESTAMP NOT NULL,
    trial_end TIMESTAMP,

    -- Cancellation
    cancel_at_period_end BOOLEAN DEFAULT false,
    canceled_at TIMESTAMP,

    -- Timestamps
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),

    UNIQUE(tenant_id)  -- One active subscription per tenant
);

CREATE INDEX idx_tenant_subscriptions_tenant ON tenant_subscriptions(tenant_id);
CREATE INDEX idx_tenant_subscriptions_status ON tenant_subscriptions(status);
CREATE INDEX idx_tenant_subscriptions_stripe ON tenant_subscriptions(stripe_subscription_id);

-- ============================================
-- Token Usage Tracking (for billing)
-- ============================================
CREATE TABLE token_usage (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(255) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    subscription_id UUID REFERENCES tenant_subscriptions(id) ON DELETE SET NULL,

    -- Knowledge base context
    knowledge_base_id VARCHAR(255),

    -- Token counts
    input_tokens BIGINT DEFAULT 0,
    output_tokens BIGINT DEFAULT 0,
    embedding_tokens BIGINT DEFAULT 0,
    total_tokens BIGINT GENERATED ALWAYS AS (input_tokens + output_tokens + embedding_tokens) STORED,

    -- Model and operation
    model_name VARCHAR(100),  -- 'gpt-4', 'claude-3-opus', 'text-embedding-ada-002'
    operation_type VARCHAR(50) CHECK (operation_type IN ('chat', 'embedding', 'search', 'completion')),

    -- Cost calculation
    estimated_cost DECIMAL(10, 6),  -- Calculated cost for this operation

    -- Stripe metering (for usage-based billing)
    stripe_usage_record_id VARCHAR(255),  -- Stripe Usage Record ID
    reported_to_stripe BOOLEAN DEFAULT false,
    reported_at TIMESTAMP,

    -- Metadata
    user_id VARCHAR(255),
    session_id UUID,
    request_id VARCHAR(100),

    -- Timestamps
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_token_usage_tenant_date ON token_usage(tenant_id, created_at DESC);
CREATE INDEX idx_token_usage_subscription ON token_usage(subscription_id);
CREATE INDEX idx_token_usage_billing_period ON token_usage(tenant_id, created_at) WHERE reported_to_stripe = false;
CREATE INDEX idx_token_usage_kb ON token_usage(knowledge_base_id);

-- ============================================
-- Invoices
-- ============================================
CREATE TABLE invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(255) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    subscription_id UUID REFERENCES tenant_subscriptions(id) ON DELETE SET NULL,

    -- Invoice details
    invoice_number VARCHAR(100) UNIQUE NOT NULL,

    -- Stripe/Adyen references
    stripe_invoice_id VARCHAR(255),
    stripe_payment_intent_id VARCHAR(255),

    -- Amounts (in cents)
    subtotal BIGINT NOT NULL,  -- Base subscription cost
    usage_charges BIGINT DEFAULT 0,  -- Overage charges
    tax BIGINT DEFAULT 0,
    total BIGINT NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',

    -- Token usage summary
    tokens_included BIGINT,  -- Tokens included in plan
    tokens_used BIGINT,  -- Total tokens used in period
    tokens_overage BIGINT GENERATED ALWAYS AS (GREATEST(tokens_used - tokens_included, 0)) STORED,

    -- Status
    status VARCHAR(50) DEFAULT 'draft' CHECK (status IN ('draft', 'open', 'paid', 'void', 'uncollectible')),

    -- Payment
    paid BOOLEAN DEFAULT false,
    paid_at TIMESTAMP,

    -- Billing period
    period_start TIMESTAMP NOT NULL,
    period_end TIMESTAMP NOT NULL,

    -- Due date
    due_date TIMESTAMP,

    -- PDF/Document
    invoice_pdf_url TEXT,

    -- Timestamps
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_invoices_tenant ON invoices(tenant_id, created_at DESC);
CREATE INDEX idx_invoices_status ON invoices(status);
CREATE INDEX idx_invoices_stripe ON invoices(stripe_invoice_id);
CREATE INDEX idx_invoices_number ON invoices(invoice_number);

-- ============================================
-- Payment Methods
-- ============================================
CREATE TABLE payment_methods (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(255) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,

    -- Stripe/Adyen references
    stripe_payment_method_id VARCHAR(255),

    -- Card details (last 4 digits, brand)
    type VARCHAR(50) CHECK (type IN ('card', 'sepa_debit', 'paypal', 'bank_transfer')),
    card_brand VARCHAR(50),  -- 'visa', 'mastercard', 'amex'
    card_last4 VARCHAR(4),
    card_exp_month INT,
    card_exp_year INT,

    -- Default payment method
    is_default BOOLEAN DEFAULT false,

    -- Status
    status VARCHAR(50) DEFAULT 'active' CHECK (status IN ('active', 'expired', 'failed')),

    -- Timestamps
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_payment_methods_tenant ON payment_methods(tenant_id);
CREATE INDEX idx_payment_methods_stripe ON payment_methods(stripe_payment_method_id);
CREATE INDEX idx_payment_methods_default ON payment_methods(tenant_id, is_default) WHERE is_default = true;

-- ============================================
-- Billing Events (Webhook Log)
-- ============================================
CREATE TABLE billing_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Event source
    provider VARCHAR(50) CHECK (provider IN ('stripe', 'adyen')),
    event_type VARCHAR(100) NOT NULL,  -- 'customer.subscription.updated', 'invoice.paid', etc.
    event_id VARCHAR(255) UNIQUE NOT NULL,  -- Provider's event ID (for idempotency)

    -- Related entities
    tenant_id VARCHAR(255),
    subscription_id UUID,
    invoice_id UUID,

    -- Payload
    payload JSONB NOT NULL,  -- Full webhook payload

    -- Processing
    processed BOOLEAN DEFAULT false,
    processed_at TIMESTAMP,
    error_message TEXT,
    retry_count INT DEFAULT 0,

    -- Timestamps
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_billing_events_event_id ON billing_events(event_id);
CREATE INDEX idx_billing_events_tenant ON billing_events(tenant_id);
CREATE INDEX idx_billing_events_type ON billing_events(event_type);
CREATE INDEX idx_billing_events_processed ON billing_events(processed, created_at) WHERE processed = false;

-- ============================================
-- Update Triggers
-- ============================================
CREATE OR REPLACE FUNCTION update_billing_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_subscription_plans_updated_at
    BEFORE UPDATE ON subscription_plans
    FOR EACH ROW
    EXECUTE FUNCTION update_billing_updated_at();

CREATE TRIGGER update_tenant_subscriptions_updated_at
    BEFORE UPDATE ON tenant_subscriptions
    FOR EACH ROW
    EXECUTE FUNCTION update_billing_updated_at();

CREATE TRIGGER update_invoices_updated_at
    BEFORE UPDATE ON invoices
    FOR EACH ROW
    EXECUTE FUNCTION update_billing_updated_at();

CREATE TRIGGER update_payment_methods_updated_at
    BEFORE UPDATE ON payment_methods
    FOR EACH ROW
    EXECUTE FUNCTION update_billing_updated_at();

-- ============================================
-- Comments
-- ============================================
COMMENT ON TABLE subscription_plans IS 'Available subscription tiers with pricing and quotas';
COMMENT ON TABLE tenant_subscriptions IS 'Active subscriptions per tenant with Stripe/Adyen references';
COMMENT ON TABLE token_usage IS 'Token consumption tracking for usage-based billing';
COMMENT ON TABLE invoices IS 'Generated invoices with subscription and usage charges';
COMMENT ON TABLE payment_methods IS 'Stored payment methods for automatic billing';
COMMENT ON TABLE billing_events IS 'Webhook event log from Stripe/Adyen for audit and retry';
