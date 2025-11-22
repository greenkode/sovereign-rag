-- Create prompt template system tables
-- This migration creates the foundation for database-driven prompt templates

-- ============================================================================
-- Table: prompt_templates
-- Stores all template text with parameter placeholders
-- Supports global templates and tenant-specific overrides
-- ============================================================================
CREATE TABLE prompt_templates (
    -- Primary Key
    id                  BIGSERIAL PRIMARY KEY,

    -- Tenant Support (NULL = global template available to all tenants)
    tenant_id           VARCHAR(255),

    -- Template Classification
    category            VARCHAR(100) NOT NULL
        CHECK (category IN ('persona', 'system', 'instruction')),
    name                VARCHAR(100) NOT NULL,
    version             INT NOT NULL DEFAULT 1,

    -- Template Content
    template_text       TEXT NOT NULL,
    parameters          JSONB,                      -- Array of parameter names: ["param1", "param2"]
    metadata            JSONB,                      -- Additional metadata (tags, description, etc.)

    -- Lifecycle
    active              BOOLEAN NOT NULL DEFAULT true,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for common query patterns
CREATE INDEX idx_prompt_category_name ON prompt_templates(category, name);
CREATE INDEX idx_prompt_tenant ON prompt_templates(tenant_id);
CREATE INDEX idx_prompt_active ON prompt_templates(active);
CREATE INDEX idx_prompt_category ON prompt_templates(category);

-- Comments
COMMENT ON TABLE prompt_templates IS 'Stores prompt templates with $${parameter} placeholders for dynamic substitution';
COMMENT ON COLUMN prompt_templates.tenant_id IS 'NULL for global templates, specific tenant_id for overrides';
COMMENT ON COLUMN prompt_templates.category IS 'Template category: persona, system, or instruction';
COMMENT ON COLUMN prompt_templates.template_text IS 'Template with $${parameter} placeholders using Apache Commons Text syntax';
COMMENT ON COLUMN prompt_templates.parameters IS 'JSON array of required parameter names';

-- ============================================================================
-- Table: persona_configurations
-- Assembles complete personas from multiple templates
-- Links base template + optional language/escalation templates
-- ============================================================================
CREATE TABLE persona_configurations (
    -- Primary Key
    id                      BIGSERIAL PRIMARY KEY,

    -- Tenant Support (NULL = global persona available to all tenants)
    tenant_id               VARCHAR(255),

    -- Persona Identity
    persona_key             VARCHAR(100) NOT NULL,
    display_name            VARCHAR(255) NOT NULL,
    description             TEXT,

    -- Template References (composable design)
    base_template_id        BIGINT NOT NULL,        -- Main persona prompt template
    language_template_id    BIGINT,                 -- Optional: language-specific instructions
    escalation_template_id  BIGINT,                 -- Optional: escalation messaging

    -- Default Parameters
    parameters              JSONB,                  -- Default parameter values as JSON object

    -- Lifecycle
    active                  BOOLEAN NOT NULL DEFAULT true,
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign Keys
    CONSTRAINT fk_base_template
        FOREIGN KEY (base_template_id)
        REFERENCES prompt_templates(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_language_template
        FOREIGN KEY (language_template_id)
        REFERENCES prompt_templates(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_escalation_template
        FOREIGN KEY (escalation_template_id)
        REFERENCES prompt_templates(id)
        ON DELETE SET NULL,

    -- Unique Constraints
    CONSTRAINT uk_persona_tenant_key
        UNIQUE (tenant_id, persona_key)
);

-- Indexes for common query patterns
CREATE INDEX idx_persona_tenant ON persona_configurations(tenant_id);
CREATE INDEX idx_persona_key ON persona_configurations(persona_key);
CREATE INDEX idx_persona_active ON persona_configurations(active);

-- Comments
COMMENT ON TABLE persona_configurations IS 'Persona configurations assembled from multiple reusable templates';
COMMENT ON COLUMN persona_configurations.persona_key IS 'Unique identifier: customer_service, professional, etc.';
COMMENT ON COLUMN persona_configurations.base_template_id IS 'Primary persona prompt template';
COMMENT ON COLUMN persona_configurations.language_template_id IS 'Optional template for language-specific instructions';
COMMENT ON COLUMN persona_configurations.escalation_template_id IS 'Optional template for escalation messaging';
COMMENT ON COLUMN persona_configurations.parameters IS 'Default parameter values as JSON: {"role": "...", "tone": "..."}';
