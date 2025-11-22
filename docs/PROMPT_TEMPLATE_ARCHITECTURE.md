# Prompt Template Architecture - Design Document

## Problem Statement

The `ConversationalAgentService` (1161 lines) has hardcoded:
- 8 persona definitions with instructions, restrictions, and escalation rules
- Multiple system prompts (language detection, confidence extraction, email parsing, etc.)
- Formatting rules and output templates
- Language-specific instructions
- Business logic mixed with prompt text

**Goal**: Extract all prompts into database-backed, configurable templates with parameter substitution.

---

## Analysis of Current Hardcoded Content

### 1. **Personas** (8 types)
- `customer_service`, `professional`, `casual`, `technical`, `concise`, `educational`, `technical_support`, `sales`
- Each contains:
  - Identity/introduction rules
  - Response style guidelines
  - Formatting rules (Markdown, bold/italic preferences)
  - Output templates
  - Escalation triggers and messaging
  - Source citation instructions

### 2. **System Prompts** (Multiple categories)
- **Language Instructions**: "CRITICAL: Respond in {language} language"
- **Confidence Extraction**: Extract `[CONFIDENCE: X%]` from responses
- **Email Extraction**: Parse JSON with user contact info
- **Satisfaction Analysis**: Determine if user seems finished
- **Language Detection**: "What language is this text written in?"
- **RAG Instructions**: How to use retrieved context

### 3. **Dynamic Parameters** (Need substitution)
- `{language}` - User's language preference
- `{context}` - Retrieved knowledge base context
- `{sources}` - Source URLs for citations
- `{conversationHistory}` - Previous messages
- `{userMessage}` - Current user query
- `{websiteName}` - Tenant-specific branding
- `{companyName}` - Tenant company name
- `{escalationEmail}` - Support email

---

## Proposed Architecture

### Database Schema

```
┌─────────────────────────────────────────────────────────────┐
│                     PROMPT TEMPLATES                        │
│─────────────────────────────────────────────────────────────│
│ id                BIGSERIAL PRIMARY KEY                     │
│ tenant_id         VARCHAR (nullable for global templates)   │
│ category          VARCHAR (persona|system|instruction)      │
│ name              VARCHAR (customer_service|language_detect)│
│ version           INT (versioning for A/B testing)          │
│ template_text     TEXT (with {{parameter}} placeholders)    │
│ parameters        JSONB (parameter definitions)             │
│ metadata          JSONB (language, tags, description)       │
│ active            BOOLEAN                                   │
│ created_at        TIMESTAMP                                 │
│ updated_at        TIMESTAMP                                 │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                  PROMPT TEMPLATE SECTIONS                   │
│─────────────────────────────────────────────────────────────│
│ id                BIGSERIAL PRIMARY KEY                     │
│ template_id       BIGINT → prompt_templates(id)            │
│ section_type      VARCHAR (identity|style|formatting|       │
│                           escalation|restrictions)          │
│ section_text      TEXT                                     │
│ sort_order        INT                                       │
│ active            BOOLEAN                                   │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                  PERSONA CONFIGURATIONS                     │
│─────────────────────────────────────────────────────────────│
│ id                BIGSERIAL PRIMARY KEY                     │
│ tenant_id         VARCHAR (nullable for global)            │
│ persona_key       VARCHAR (customer_service, etc.)         │
│ display_name      VARCHAR                                   │
│ description       TEXT                                      │
│ base_template_id  BIGINT → prompt_templates(id)            │
│ language_template_id BIGINT (optional language override)    │
│ escalation_template_id BIGINT (escalation messaging)        │
│ active            BOOLEAN                                   │
│ created_at        TIMESTAMP                                 │
│ updated_at        TIMESTAMP                                 │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│               TEMPLATE PARAMETER DEFINITIONS                │
│─────────────────────────────────────────────────────────────│
│ id                BIGSERIAL PRIMARY KEY                     │
│ parameter_name    VARCHAR (language, context, sources)      │
│ parameter_type    VARCHAR (string|list|object|number)       │
│ required          BOOLEAN                                   │
│ default_value     TEXT                                      │
│ description       TEXT                                      │
│ validation_rules  JSONB                                     │
└─────────────────────────────────────────────────────────────┘
```

### Parameter Substitution Engine

```kotlin
interface PromptTemplateRenderer {
    /**
     * Render a template with parameter substitution
     *
     * @param template The template text with {{param}} placeholders
     * @param parameters Map of parameter names to values
     * @return Rendered text with parameters substituted
     */
    fun render(template: String, parameters: Map<String, Any>): String
}

// Support for:
// - Simple substitution: {{language}}
// - Conditional blocks: {{#if showSources}}...{{/if}}
// - Lists: {{#each sources}}[{{name}}]({{url}}){{/each}}
// - Nested parameters: {{user.email}}
```

---

## Implementation Checklist

### Phase 1: Database Foundation
- [ ] Create database schema (migrations)
  - [ ] `prompt_templates` table
  - [ ] `prompt_template_sections` table
  - [ ] `persona_configurations` table
  - [ ] `template_parameter_definitions` table
- [ ] Create JPA entities
  - [ ] `PromptTemplate`
  - [ ] `PromptTemplateSection`
  - [ ] `PersonaConfiguration`
  - [ ] `TemplateParameterDefinition`
- [ ] Create repositories
  - [ ] `PromptTemplateRepository`
  - [ ] `PersonaConfigurationRepository`

### Phase 2: Template Engine
- [ ] Create `PromptTemplateRenderer` interface in commons
- [ ] Implement `HandlebarsTemplateRenderer` in new module
  - [ ] Support {{parameter}} substitution
  - [ ] Support {{#if}} conditionals
  - [ ] Support {{#each}} loops
  - [ ] Escape/sanitize inputs
- [ ] Create `PromptTemplateService`
  - [ ] Load templates from database
  - [ ] Cache compiled templates
  - [ ] Handle tenant-specific overrides
  - [ ] Fall back to global templates

### Phase 3: Persona System Refactoring
- [ ] Create `PersonaService`
  - [ ] Load persona configurations
  - [ ] Resolve persona templates (base + language + escalation)
  - [ ] Render complete persona prompts
- [ ] Migrate existing personas to database
  - [ ] Seed migration for 8 default personas
  - [ ] Extract sections (identity, style, formatting, escalation)
- [ ] Update `ConversationalAgentService`
  - [ ] Replace `personas` map with `PersonaService`
  - [ ] Remove `getPersonaPrompt()` method
  - [ ] Use `personaService.renderPersona(persona, params)`

### Phase 4: System Prompts Migration
- [ ] Create template categories
  - [ ] `system/language_instruction`
  - [ ] `system/confidence_extraction`
  - [ ] `system/email_extraction`
  - [ ] `system/satisfaction_analysis`
  - [ ] `system/language_detection`
- [ ] Migrate hardcoded prompts to database
- [ ] Update all prompt usages in `ConversationalAgentService`
  - [ ] `getLanguageInstruction()` → `templateService.render("system/language_instruction", params)`
  - [ ] Inline prompts → Template lookups

### Phase 5: Testing & Validation
- [ ] Unit tests for `PromptTemplateRenderer`
  - [ ] Parameter substitution
  - [ ] Conditional blocks
  - [ ] List iteration
  - [ ] Error handling
- [ ] Integration tests for `PromptTemplateService`
  - [ ] Template loading
  - [ ] Caching
  - [ ] Tenant override resolution
- [ ] E2E tests for persona rendering
- [ ] Migration validation
  - [ ] Verify all existing personas work identically

### Phase 6: Admin UI (Future)
- [ ] Create REST API for template management
  - [ ] GET /api/admin/templates
  - [ ] POST /api/admin/templates
  - [ ] PUT /api/admin/templates/{id}
  - [ ] GET /api/admin/personas
- [ ] Admin interface for template editing
  - [ ] Template editor with syntax highlighting
  - [ ] Parameter documentation
  - [ ] Preview/test rendering
  - [ ] Version history

---

## Template Examples

### Persona Template (Base)
```handlebars
You are a {{role}} for this website.

IMPORTANT - When asked "who are you?" or similar identity questions:
- Identify yourself as an AI assistant helping visitors with this website
- Explain that you can answer questions using the website's knowledge base
- Keep it {{tone}}

Your responses should be:
- CONCISE ({{maxSentences}} sentences maximum)
- {{styleAttributes}}
- Formatted in Markdown
{{#if includeSources}}
- Include source links when referencing knowledge base facts
{{/if}}

{{#if formattingRules}}
FORMATTING RULES (MUST FOLLOW):
{{#each formattingRules}}
- {{this}}
{{/each}}
{{/if}}

{{#if escalation}}
ESCALATION - When to offer human support:
Proactively offer to connect users with a human team member when you detect:
{{#each escalation.triggers}}
- {{this}}
{{/each}}

When you detect these signals, respond with:
"{{escalation.message}}"
{{/if}}
```

### Language Instruction Template
```handlebars
{{#if language}}

CRITICAL: Respond in {{languageName}} language. The user's interface is set to {{languageName}}.
{{/if}}
```

### Confidence Extraction Template
```handlebars
Analyze this query and return your confidence level (0-100%) that you can answer it well using the knowledge base.

Query: "{{query}}"

Available context:
{{context}}

Format: [CONFIDENCE: X%]
```

---

## Benefits of This Architecture

### Flexibility
- ✅ Tenants can customize personas without code changes
- ✅ A/B test different prompt variations
- ✅ Multi-language persona definitions
- ✅ Real-time updates without deployment

### Maintainability
- ✅ Centralized prompt management
- ✅ Version control for prompts
- ✅ Clear separation of logic and content
- ✅ Reduced code complexity (1161 → ~300 lines)

### Scalability
- ✅ Template caching for performance
- ✅ Tenant-specific overrides
- ✅ Easy to add new personas/prompts
- ✅ Support for multi-tenancy

### Observability
- ✅ Track which prompts are used
- ✅ A/B testing metrics
- ✅ Audit trail for template changes
- ✅ Debug rendered prompts

---

## Migration Strategy

1. **Parallel Development**: Build new system alongside existing
2. **Feature Flag**: Use flag to toggle between old/new system
3. **Gradual Migration**: Migrate one category at a time:
   - Start with system prompts (simpler, less risk)
   - Then migrate personas (more complex)
4. **Validation**: Compare outputs between old and new system
5. **Deprecation**: Remove old hardcoded system after validation

---

## Example Usage

### Before (Current)
```kotlin
val personaPrompt = personas["customer_service"]!!
val languageInstr = getLanguageInstruction(session.language)
val systemPrompt = personaPrompt + languageInstr
```

### After (New)
```kotlin
val systemPrompt = personaService.renderPersona(
    personaKey = "customer_service",
    parameters = mapOf(
        "language" to session.language,
        "languageName" to getLanguageName(session.language),
        "includeSources" to showSources,
        "websiteName" to tenant.name
    )
)
```

---

## Next Steps

Should I proceed with implementation starting with Phase 1 (Database Foundation)?
