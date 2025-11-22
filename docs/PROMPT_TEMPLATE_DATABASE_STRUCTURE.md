# Prompt Template Database Structure

## Complete Table Schemas

### 1. `prompt_templates` - Core Template Storage

```sql
CREATE TABLE prompt_templates (
    -- Primary Key
    id                  BIGSERIAL PRIMARY KEY,

    -- Tenant Support (NULL = global template)
    tenant_id           VARCHAR(255),

    -- Template Classification
    category            VARCHAR(100) NOT NULL,     -- 'persona' | 'system' | 'instruction'
    name                VARCHAR(100) NOT NULL,     -- 'customer_service' | 'language_instruction'
    version             INT NOT NULL DEFAULT 1,    -- For A/B testing and rollback

    -- Template Content
    template_text       TEXT NOT NULL,             -- Template with ${parameter} placeholders
    parameters          JSONB,                     -- List of required parameters
    metadata            JSONB,                     -- Additional config (language, tags, etc.)

    -- Lifecycle
    active              BOOLEAN NOT NULL DEFAULT true,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Indexes
    INDEX idx_prompt_category_name (category, name),
    INDEX idx_prompt_tenant (tenant_id),
    INDEX idx_prompt_active (active),

    -- Constraints
    CHECK (category IN ('persona', 'system', 'instruction'))
);
```

**Example Data:**

| id | tenant_id | category | name | version | template_text | parameters | active |
|----|-----------|----------|------|---------|---------------|------------|--------|
| 1 | NULL | persona | customer_service_base | 1 | `You are a helpful ${role}...` | `["role", "tone"]` | true |
| 2 | NULL | system | language_instruction | 1 | `CRITICAL: Respond in ${languageName}...` | `["language", "languageName"]` | true |
| 3 | NULL | system | confidence_extraction | 1 | `Analyze this query: "${query}"...` | `["query", "context"]` | true |
| 4 | dev | persona | customer_service_base | 1 | `You are a super friendly ${role}...` | `["role", "tone"]` | true |

**Row 4 shows tenant override**: Tenant "dev" has custom version that overrides global template (id=1)

---

### 2. `persona_configurations` - Persona Assembly

```sql
CREATE TABLE persona_configurations (
    -- Primary Key
    id                      BIGSERIAL PRIMARY KEY,

    -- Tenant Support (NULL = global persona)
    tenant_id               VARCHAR(255),

    -- Persona Identity
    persona_key             VARCHAR(100) NOT NULL,     -- 'customer_service', 'professional'
    display_name            VARCHAR(255) NOT NULL,     -- 'Customer Service', 'Professional'
    description             TEXT,

    -- Template References (FK to prompt_templates)
    base_template_id        BIGINT NOT NULL,           -- Main persona prompt
    language_template_id    BIGINT,                    -- Optional: language instructions
    escalation_template_id  BIGINT,                    -- Optional: escalation messaging

    -- Default Parameters
    parameters              JSONB,                     -- Default values for parameters

    -- Lifecycle
    active                  BOOLEAN NOT NULL DEFAULT true,
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign Keys
    CONSTRAINT fk_base_template
        FOREIGN KEY (base_template_id) REFERENCES prompt_templates(id) ON DELETE RESTRICT,
    CONSTRAINT fk_language_template
        FOREIGN KEY (language_template_id) REFERENCES prompt_templates(id) ON DELETE SET NULL,
    CONSTRAINT fk_escalation_template
        FOREIGN KEY (escalation_template_id) REFERENCES prompt_templates(id) ON DELETE SET NULL,

    -- Unique Constraints
    CONSTRAINT uk_persona_tenant_key UNIQUE (tenant_id, persona_key),

    -- Indexes
    INDEX idx_persona_tenant (tenant_id),
    INDEX idx_persona_key (persona_key),
    INDEX idx_persona_active (active)
);
```

**Example Data:**

| id | tenant_id | persona_key | display_name | base_template_id | language_template_id | escalation_template_id | parameters |
|----|-----------|-------------|--------------|------------------|---------------------|----------------------|------------|
| 1 | NULL | customer_service | Customer Service | 1 | 2 | 10 | `{"role": "customer service representative", "tone": "friendly", "maxSentences": 3}` |
| 2 | NULL | professional | Professional | 5 | 2 | 11 | `{"role": "professional expert", "tone": "formal", "maxSentences": 4}` |
| 3 | NULL | casual | Casual | 6 | 2 | 12 | `{"role": "friendly helper", "tone": "casual", "maxSentences": 3}` |
| 4 | dev | customer_service | Dev Customer Service | 4 | 2 | 10 | `{"role": "super friendly assistant", "tone": "very friendly"}` |

**Row 4 shows tenant override**: Tenant "dev" has custom customer_service persona

---

## Complete Data Flow Example

### Scenario: Render "customer_service" persona for tenant "dev" with Dutch language

**Step 1: Load Persona Configuration**
```sql
SELECT * FROM persona_configurations
WHERE persona_key = 'customer_service'
  AND (tenant_id = 'dev' OR tenant_id IS NULL)
ORDER BY tenant_id DESC NULLS LAST
LIMIT 1;
```

**Result:**
```
id: 4
persona_key: customer_service
base_template_id: 4  (tenant override)
language_template_id: 2
parameters: {"role": "super friendly assistant", "tone": "very friendly"}
```

**Step 2: Load Base Template (id=4)**
```sql
SELECT * FROM prompt_templates WHERE id = 4;
```

**Result:**
```
template_text:
"You are a ${role} for this website.

Your responses should be:
- CONCISE (${maxSentences} sentences maximum)
- ${tone}
- Formatted in Markdown

${includeSources}"
```

**Step 3: Load Language Template (id=2)**
```sql
SELECT * FROM prompt_templates WHERE id = 2;
```

**Result:**
```
template_text:
"CRITICAL: Respond in ${languageName} language. The user's interface is set to ${languageName}."
```

**Step 4: Merge Parameters**
```kotlin
val defaultParams = parseJson(persona.parameters)
// {"role": "super friendly assistant", "tone": "very friendly"}

val runtimeParams = mapOf(
    "language" to "nl",
    "languageName" to "Dutch",
    "maxSentences" to 3,
    "includeSources" to "- Include source links when referencing knowledge base facts"
)

val allParams = defaultParams + runtimeParams
// {"role": "super friendly assistant", "tone": "very friendly",
//  "language": "nl", "languageName": "Dutch", "maxSentences": 3, ...}
```

**Step 5: Render Base Template**
```kotlin
val basePrompt = renderer.render(baseTemplate.templateText, allParams)
```

**Result:**
```
"You are a super friendly assistant for this website.

Your responses should be:
- CONCISE (3 sentences maximum)
- very friendly
- Formatted in Markdown

- Include source links when referencing knowledge base facts"
```

**Step 6: Render Language Template**
```kotlin
val languagePrompt = renderer.render(languageTemplate.templateText, allParams)
```

**Result:**
```
"CRITICAL: Respond in Dutch language. The user's interface is set to Dutch."
```

**Step 7: Combine**
```kotlin
val finalPrompt = basePrompt + "\n\n" + languagePrompt
```

**Final Result:**
```
"You are a super friendly assistant for this website.

Your responses should be:
- CONCISE (3 sentences maximum)
- very friendly
- Formatted in Markdown

- Include source links when referencing knowledge base facts

CRITICAL: Respond in Dutch language. The user's interface is set to Dutch."
```

---

## System Template Examples

### Template: `system/language_instruction`

```sql
INSERT INTO prompt_templates (category, name, template_text, parameters, metadata, active)
VALUES (
    'system',
    'language_instruction',
    'CRITICAL: Respond in ${languageName} language. The user''s interface is set to ${languageName}.',
    '["language", "languageName"]',
    '{"description": "Language-specific instructions for responses", "version": "1.0"}',
    true
);
```

**Usage:**
```kotlin
templateService.renderTemplate(
    category = "system",
    name = "language_instruction",
    parameters = mapOf(
        "language" to "nl",
        "languageName" to "Dutch"
    )
)
// → "CRITICAL: Respond in Dutch language. The user's interface is set to Dutch."
```

---

### Template: `system/confidence_extraction`

```sql
INSERT INTO prompt_templates (category, name, template_text, parameters, metadata, active)
VALUES (
    'system',
    'confidence_extraction',
    'Analyze this query and return your confidence level (0-100%) that you can answer it well using the knowledge base.

Query: "${query}"

Available context:
${context}

Format your response as: [CONFIDENCE: X%]',
    '["query", "context"]',
    '{"description": "Extract confidence score from AI response", "version": "1.0"}',
    true
);
```

**Usage:**
```kotlin
templateService.renderTemplate(
    category = "system",
    name = "confidence_extraction",
    parameters = mapOf(
        "query" to "What are your opening hours?",
        "context" to "We are open Monday-Friday 9am-5pm..."
    )
)
```

---

### Template: `system/email_extraction`

```sql
INSERT INTO prompt_templates (category, name, template_text, parameters, metadata, active)
VALUES (
    'system',
    'email_extraction',
    'Extract user contact information from this conversation and format as JSON.

Conversation:
${conversationHistory}

Extract:
- email (required)
- name (optional)
- phone (optional)
- message describing their issue (optional)

Format: {"email": "...", "name": "...", "phone": "...", "message": "..."}',
    '["conversationHistory"]',
    '{"description": "Parse user contact info for escalation", "version": "1.0"}',
    true
);
```

---

## Tenant Override Strategy

### Global Templates (tenant_id = NULL)
```sql
-- Available to ALL tenants
INSERT INTO prompt_templates (tenant_id, category, name, template_text, active)
VALUES (NULL, 'persona', 'customer_service_base', '...', true);
```

### Tenant-Specific Override (tenant_id = 'acme')
```sql
-- Only for tenant 'acme', overrides global
INSERT INTO prompt_templates (tenant_id, category, name, template_text, active)
VALUES ('acme', 'persona', 'customer_service_base', '...custom...', true);
```

### Query Logic (Automatic Priority)
```sql
-- This query automatically picks tenant override if it exists
SELECT * FROM prompt_templates
WHERE category = :category
  AND name = :name
  AND (tenant_id = :tenantId OR tenant_id IS NULL)
ORDER BY tenant_id DESC NULLS LAST  -- tenant_id NOT NULL comes first
LIMIT 1;
```

**Result Priority:**
1. If `tenant_id = 'acme'` exists → Use it
2. If `tenant_id = 'acme'` doesn't exist → Use `tenant_id = NULL` (global)

---

## Version Management

### Multiple Versions for A/B Testing

```sql
-- Version 1 (active)
INSERT INTO prompt_templates (category, name, version, template_text, active)
VALUES ('persona', 'customer_service_base', 1, 'You are helpful...', true);

-- Version 2 (testing)
INSERT INTO prompt_templates (category, name, version, template_text, active)
VALUES ('persona', 'customer_service_base', 2, 'You are SUPER helpful...', false);
```

**Query for Active Version:**
```sql
SELECT * FROM prompt_templates
WHERE category = 'persona'
  AND name = 'customer_service_base'
  AND active = true
ORDER BY version DESC
LIMIT 1;
```

**Enable Version 2:**
```sql
UPDATE prompt_templates SET active = false WHERE id = 1;  -- Disable v1
UPDATE prompt_templates SET active = true WHERE id = 2;   -- Enable v2
```

---

## Parameters JSON Structure

### In `prompt_templates.parameters`
```json
["role", "tone", "maxSentences", "includeSources"]
```

### In `persona_configurations.parameters`
```json
{
  "role": "customer service representative",
  "tone": "friendly and professional",
  "maxSentences": 3,
  "formattingRules": [
    "ALWAYS use **bold** for names and key terms",
    "NEVER use italic (*) formatting",
    "Keep formatting consistent"
  ],
  "escalation": {
    "triggers": [
      "User expresses frustration",
      "Multiple failed attempts",
      "User explicitly asks for help"
    ],
    "message": "I apologize that I'm having trouble helping you with this. Would you like me to connect you with a team member?"
  }
}
```

---

## Metadata JSON Structure

```json
{
  "description": "Customer service persona with friendly tone",
  "language": "en",
  "tags": ["customer-service", "support", "default"],
  "version": "1.0",
  "author": "admin@sovereignrag.ai",
  "createdDate": "2025-01-01",
  "usageCount": 1234,
  "abTestGroup": "A"
}
```

---

## Entity Relationships Diagram

```
┌─────────────────────────────────────────────┐
│         prompt_templates (id=1-100)         │
│─────────────────────────────────────────────│
│ id, tenant_id, category, name,              │
│ version, template_text, parameters          │
└─────────────────────────────────────────────┘
          ▲
          │
          │ FK: base_template_id
          │ FK: language_template_id
          │ FK: escalation_template_id
          │
┌─────────────────────────────────────────────┐
│      persona_configurations (id=1-20)       │
│─────────────────────────────────────────────│
│ id, tenant_id, persona_key,                 │
│ base_template_id,                           │
│ language_template_id,                       │
│ escalation_template_id,                     │
│ parameters                                  │
└─────────────────────────────────────────────┘
```

**One persona** → References **3 templates** (base + language + escalation)

---

## Index Strategy

### Query Patterns and Indexes

**Pattern 1: Load template by category/name**
```sql
-- Index: idx_prompt_category_name (category, name)
SELECT * FROM prompt_templates
WHERE category = 'persona' AND name = 'customer_service_base';
```

**Pattern 2: Tenant override resolution**
```sql
-- Index: idx_prompt_tenant (tenant_id)
SELECT * FROM prompt_templates
WHERE category = 'persona'
  AND name = 'customer_service_base'
  AND (tenant_id = 'dev' OR tenant_id IS NULL);
```

**Pattern 3: Load persona by key**
```sql
-- Index: idx_persona_key (persona_key)
SELECT * FROM persona_configurations
WHERE persona_key = 'customer_service'
  AND (tenant_id = 'dev' OR tenant_id IS NULL);
```

**Pattern 4: List all active templates**
```sql
-- Index: idx_prompt_active (active)
SELECT * FROM prompt_templates
WHERE active = true;
```

---

## Storage Estimates

### Assumptions:
- 8 personas × 3 templates each = 24 persona templates
- 10 system templates
- 100 tenants with 10% custom override rate
- Average template size: 500 bytes

**Storage:**
```
Global templates: 34 × 500 bytes = 17 KB
Tenant overrides: 100 × 0.1 × 34 × 500 bytes = 170 KB
Persona configs: 8 × 100 tenants × 200 bytes = 160 KB

Total: ~350 KB (negligible)
```

---

## Migration Rollout Strategy

### Step 1: Create Tables
```sql
-- V100__create_prompt_template_tables.sql
CREATE TABLE prompt_templates (...);
CREATE TABLE persona_configurations (...);
```

### Step 2: Seed Global Templates
```sql
-- V101__seed_global_templates.sql
INSERT INTO prompt_templates (...) -- 34 templates
INSERT INTO persona_configurations (...) -- 8 personas
```

### Step 3: Migrate Existing Tenants (if any custom)
```sql
-- V102__migrate_tenant_customizations.sql
-- Copy any existing tenant-specific customizations
```

---

## Sample Complete Seed Data

```sql
-- Base persona template
INSERT INTO prompt_templates (category, name, template_text, parameters) VALUES
('persona', 'customer_service_base',
 'You are a helpful ${role} for this website.

Your responses should be:
- CONCISE (${maxSentences} sentences maximum)
- ${tone}
- Formatted in Markdown

${includeSources}',
 '["role", "tone", "maxSentences", "includeSources"]');

-- Language instruction template
INSERT INTO prompt_templates (category, name, template_text, parameters) VALUES
('system', 'language_instruction',
 'CRITICAL: Respond in ${languageName} language.',
 '["languageName"]');

-- Escalation template
INSERT INTO prompt_templates (category, name, template_text, parameters) VALUES
('instruction', 'escalation_message',
 'I apologize that I''m having trouble helping you with this. Would you like me to connect you with ${supportTeam}?',
 '["supportTeam"]');

-- Persona configuration
INSERT INTO persona_configurations
(persona_key, display_name, base_template_id, language_template_id, escalation_template_id, parameters)
VALUES
('customer_service',
 'Customer Service',
 (SELECT id FROM prompt_templates WHERE name = 'customer_service_base'),
 (SELECT id FROM prompt_templates WHERE name = 'language_instruction'),
 (SELECT id FROM prompt_templates WHERE name = 'escalation_message'),
 '{"role": "customer service representative", "tone": "friendly", "maxSentences": 3, "supportTeam": "a team member"}');
```

---

This structure gives you:
✅ **Flexibility**: Easy tenant customization
✅ **Composability**: Build personas from reusable templates
✅ **Versioning**: A/B testing and rollback
✅ **Performance**: Optimized indexes for all query patterns
✅ **Scalability**: Minimal storage overhead

Should I proceed with implementation?
