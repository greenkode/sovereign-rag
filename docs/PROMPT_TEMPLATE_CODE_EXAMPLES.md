# Prompt Template System - Code Examples

## 1. Database Entities

### PromptTemplate.kt
```kotlin
package nl.compilot.ai.prompt.domain

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(
    name = "prompt_templates",
    indexes = [
        Index(name = "idx_prompt_category_name", columnList = "category,name"),
        Index(name = "idx_prompt_tenant", columnList = "tenant_id")
    ]
)
data class PromptTemplate(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "tenant_id", length = 255)
    val tenantId: String? = null,  // null = global template

    @Column(nullable = false, length = 100)
    val category: String,  // "persona", "system", "instruction"

    @Column(nullable = false, length = 100)
    val name: String,  // "customer_service", "language_detection"

    @Column(nullable = false)
    val version: Int = 1,

    @Column(name = "template_text", nullable = false, columnDefinition = "TEXT")
    val templateText: String,

    @Column(columnDefinition = "JSONB")
    val parameters: String? = null,  // JSON array of parameter names

    @Column(columnDefinition = "JSONB")
    val metadata: String? = null,  // Additional metadata

    @Column(nullable = false)
    val active: Boolean = true,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
) {
    companion object {
        // Categories
        const val CATEGORY_PERSONA = "persona"
        const val CATEGORY_SYSTEM = "system"
        const val CATEGORY_INSTRUCTION = "instruction"
    }
}
```

### PersonaConfiguration.kt
```kotlin
package nl.compilot.ai.prompt.domain

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(
    name = "persona_configurations",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_persona_tenant_key", columnNames = ["tenant_id", "persona_key"])
    ],
    indexes = [
        Index(name = "idx_persona_tenant", columnList = "tenant_id"),
        Index(name = "idx_persona_key", columnList = "persona_key")
    ]
)
data class PersonaConfiguration(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "tenant_id", length = 255)
    val tenantId: String? = null,  // null = global persona

    @Column(name = "persona_key", nullable = false, length = 100)
    val personaKey: String,  // "customer_service", "professional", etc.

    @Column(name = "display_name", nullable = false, length = 255)
    val displayName: String,

    @Column(columnDefinition = "TEXT")
    val description: String? = null,

    @Column(name = "base_template_id", nullable = false)
    val baseTemplateId: Long,  // FK to prompt_templates

    @Column(name = "language_template_id")
    val languageTemplateId: Long? = null,  // Optional language instruction override

    @Column(name = "escalation_template_id")
    val escalationTemplateId: Long? = null,  // Optional escalation messaging override

    @Column(columnDefinition = "JSONB")
    val parameters: String? = null,  // Default parameter values as JSON

    @Column(nullable = false)
    val active: Boolean = true,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: Instant = Instant.now()
)
```

---

## 2. Repositories

### PromptTemplateRepository.kt
```kotlin
package nl.compilot.ai.prompt.repository

import nl.compilot.ai.prompt.domain.PromptTemplate
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface PromptTemplateRepository : JpaRepository<PromptTemplate, Long> {

    /**
     * Find active template by category and name
     * Tenant-specific templates override global templates
     */
    @Query("""
        SELECT pt FROM PromptTemplate pt
        WHERE pt.category = :category
          AND pt.name = :name
          AND pt.active = true
          AND (pt.tenantId = :tenantId OR pt.tenantId IS NULL)
        ORDER BY pt.tenantId DESC NULLS LAST, pt.version DESC
    """)
    fun findByCategoryAndName(
        category: String,
        name: String,
        tenantId: String?
    ): Optional<PromptTemplate>

    /**
     * Find all templates in a category
     */
    fun findByCategoryAndActiveTrue(category: String): List<PromptTemplate>

    /**
     * Find tenant-specific templates
     */
    fun findByTenantIdAndActiveTrue(tenantId: String): List<PromptTemplate>
}
```

### PersonaConfigurationRepository.kt
```kotlin
package nl.compilot.ai.prompt.repository

import nl.compilot.ai.prompt.domain.PersonaConfiguration
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface PersonaConfigurationRepository : JpaRepository<PersonaConfiguration, Long> {

    /**
     * Find persona by key with tenant override support
     */
    @Query("""
        SELECT pc FROM PersonaConfiguration pc
        WHERE pc.personaKey = :personaKey
          AND pc.active = true
          AND (pc.tenantId = :tenantId OR pc.tenantId IS NULL)
        ORDER BY pc.tenantId DESC NULLS LAST
    """)
    fun findByPersonaKey(
        personaKey: String,
        tenantId: String?
    ): Optional<PersonaConfiguration>

    /**
     * Find all active personas
     */
    fun findByActiveTrueOrderByDisplayName(): List<PersonaConfiguration>

    /**
     * Find tenant-specific personas
     */
    fun findByTenantIdAndActiveTrueOrderByDisplayName(tenantId: String): List<PersonaConfiguration>
}
```

---

## 3. Template Renderer (Using Apache Commons Text)

### PromptTemplateRenderer.kt (Interface in commons)
```kotlin
package nl.compilot.ai.commons.prompt

/**
 * Interface for rendering prompt templates with parameter substitution
 */
interface PromptTemplateRenderer {
    /**
     * Render a template with parameters
     *
     * @param template Template text with ${parameter} placeholders
     * @param parameters Map of parameter names to values
     * @return Rendered text with substituted parameters
     */
    fun render(template: String, parameters: Map<String, Any>): String

    /**
     * Validate that a template is syntactically correct
     *
     * @param template Template text to validate
     * @return List of validation errors (empty if valid)
     */
    fun validate(template: String): List<String>
}
```

### StringSubstitutorRenderer.kt (Implementation)
```kotlin
package nl.compilot.ai.prompt.service

import mu.KotlinLogging
import nl.compilot.ai.commons.prompt.PromptTemplateRenderer
import org.apache.commons.text.StringSubstitutor
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * Template renderer using Apache Commons Text StringSubstitutor
 *
 * Supports:
 * - Simple substitution: ${parameter}
 * - Nested properties: ${user.email}
 * - Default values: ${name:-Unknown}
 * - Escaping: $${literal}
 */
@Component
class StringSubstitutorRenderer : PromptTemplateRenderer {

    override fun render(template: String, parameters: Map<String, Any>): String {
        return try {
            // Convert parameters to String-keyed map for StringSubstitutor
            val stringParams = flattenParameters(parameters)

            // Create substitutor with default value support
            val substitutor = StringSubstitutor(stringParams).apply {
                isEnableSubstitutionInVariables = true  // Support nested ${${var}}
            }

            val result = substitutor.replace(template)

            logger.debug { "Rendered template with ${parameters.size} parameters" }
            result

        } catch (e: Exception) {
            logger.error(e) { "Failed to render template: ${e.message}" }
            throw TemplateRenderException("Template rendering failed: ${e.message}", e)
        }
    }

    override fun validate(template: String): List<String> {
        val errors = mutableListOf<String>()

        try {
            // Check for unmatched braces
            var braceCount = 0
            var inVariable = false
            for (i in template.indices) {
                when {
                    i < template.length - 1 && template[i] == '$' && template[i + 1] == '{' -> {
                        braceCount++
                        inVariable = true
                    }
                    template[i] == '}' && inVariable -> {
                        braceCount--
                        if (braceCount == 0) inVariable = false
                    }
                }
            }

            if (braceCount != 0) {
                errors.add("Unmatched braces in template")
            }

            // Check for empty variable names
            if (template.contains("\${}")) {
                errors.add("Empty variable name found: \${}")
            }

        } catch (e: Exception) {
            errors.add("Template validation error: ${e.message}")
        }

        return errors
    }

    /**
     * Flatten nested parameters into dot-notation
     *
     * Example: mapOf("user" to mapOf("email" to "test@example.com"))
     * Becomes: mapOf("user.email" to "test@example.com")
     */
    private fun flattenParameters(params: Map<String, Any>, prefix: String = ""): Map<String, String> {
        val flattened = mutableMapOf<String, String>()

        for ((key, value) in params) {
            val fullKey = if (prefix.isEmpty()) key else "$prefix.$key"

            when (value) {
                is Map<*, *> -> {
                    // Recursively flatten nested maps
                    @Suppress("UNCHECKED_CAST")
                    flattened.putAll(flattenParameters(value as Map<String, Any>, fullKey))
                }
                is List<*> -> {
                    // Convert lists to comma-separated strings
                    flattened[fullKey] = value.joinToString(", ")
                }
                else -> {
                    // Convert to string
                    flattened[fullKey] = value.toString()
                }
            }
        }

        return flattened
    }
}

class TemplateRenderException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
```

---

## 4. Template Service

### PromptTemplateService.kt
```kotlin
package nl.compilot.ai.prompt.service

import mu.KotlinLogging
import nl.compilot.ai.commons.prompt.PromptTemplateRenderer
import nl.compilot.ai.commons.tenant.TenantContext
import nl.compilot.ai.prompt.domain.PromptTemplate
import nl.compilot.ai.prompt.repository.PromptTemplateRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
@Transactional(readOnly = true)
class PromptTemplateService(
    private val templateRepository: PromptTemplateRepository,
    private val renderer: PromptTemplateRenderer
) {

    /**
     * Load and render a template
     *
     * @param category Template category (persona, system, instruction)
     * @param name Template name
     * @param parameters Parameters for substitution
     * @return Rendered template text
     */
    @Cacheable(cacheNames = ["prompt-templates"], key = "#category + ':' + #name + ':' + #tenantId")
    fun loadTemplate(
        category: String,
        name: String,
        tenantId: String? = TenantContext.getCurrentTenantId()
    ): PromptTemplate {
        return templateRepository.findByCategoryAndName(category, name, tenantId)
            .orElseThrow {
                PromptTemplateNotFoundException("Template not found: $category/$name for tenant $tenantId")
            }
    }

    /**
     * Render a template with parameters
     */
    fun renderTemplate(
        category: String,
        name: String,
        parameters: Map<String, Any> = emptyMap(),
        tenantId: String? = TenantContext.getCurrentTenantId()
    ): String {
        val template = loadTemplate(category, name, tenantId)

        logger.debug { "Rendering template: $category/$name with ${parameters.size} parameters" }

        return renderer.render(template.templateText, parameters)
    }

    /**
     * Create or update a template
     */
    @Transactional
    fun saveTemplate(template: PromptTemplate): PromptTemplate {
        // Validate template before saving
        val errors = renderer.validate(template.templateText)
        if (errors.isNotEmpty()) {
            throw TemplateValidationException("Template validation failed: ${errors.joinToString(", ")}")
        }

        return templateRepository.save(template)
    }

    /**
     * Get all templates in a category
     */
    fun getTemplatesByCategory(category: String): List<PromptTemplate> {
        return templateRepository.findByCategoryAndActiveTrue(category)
    }
}

class PromptTemplateNotFoundException(message: String) : RuntimeException(message)
class TemplateValidationException(message: String) : RuntimeException(message)
```

---

## 5. Persona Service

### PersonaService.kt
```kotlin
package nl.compilot.ai.prompt.service

import mu.KotlinLogging
import nl.compilot.ai.commons.tenant.TenantContext
import nl.compilot.ai.prompt.domain.PersonaConfiguration
import nl.compilot.ai.prompt.repository.PersonaConfigurationRepository
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val logger = KotlinLogging.logger {}

@Service
@Transactional(readOnly = true)
class PersonaService(
    private val personaRepository: PersonaConfigurationRepository,
    private val templateService: PromptTemplateService
) {

    /**
     * Render a complete persona prompt
     *
     * Combines:
     * - Base persona template
     * - Language instructions (if specified)
     * - Escalation messaging (if configured)
     */
    fun renderPersona(
        personaKey: String,
        parameters: Map<String, Any> = emptyMap(),
        tenantId: String? = TenantContext.getCurrentTenantId()
    ): String {
        val persona = loadPersona(personaKey, tenantId)

        // Build complete parameter map with defaults from persona config
        val allParameters = buildParameterMap(persona, parameters)

        // Render base persona template
        val basePrompt = renderTemplate(persona.baseTemplateId, allParameters)

        // Add language instructions if language specified
        val languagePrompt = if (parameters.containsKey("language") && persona.languageTemplateId != null) {
            "\n\n" + renderTemplate(persona.languageTemplateId!!, allParameters)
        } else {
            ""
        }

        // Combine prompts
        return basePrompt + languagePrompt
    }

    /**
     * Load persona configuration with caching
     */
    @Cacheable(cacheNames = ["personas"], key = "#personaKey + ':' + #tenantId")
    fun loadPersona(
        personaKey: String,
        tenantId: String? = TenantContext.getCurrentTenantId()
    ): PersonaConfiguration {
        return personaRepository.findByPersonaKey(personaKey, tenantId)
            .orElseThrow {
                PersonaNotFoundException("Persona not found: $personaKey for tenant $tenantId")
            }
    }

    /**
     * Get all available personas
     */
    fun getAllPersonas(): List<PersonaConfiguration> {
        return personaRepository.findByActiveTrueOrderByDisplayName()
    }

    /**
     * Build complete parameter map merging defaults from persona with provided params
     */
    private fun buildParameterMap(
        persona: PersonaConfiguration,
        providedParams: Map<String, Any>
    ): Map<String, Any> {
        // TODO: Parse persona.parameters JSON and merge with providedParams
        // For now, just use provided params
        return providedParams
    }

    /**
     * Render a template by ID
     */
    private fun renderTemplate(templateId: Long, parameters: Map<String, Any>): String {
        // Load template by ID and render
        // (Simplified - in reality would cache template loading)
        val template = templateService.loadTemplate("persona", "temp", null) // Placeholder
        return templateService.renderTemplate(
            category = template.category,
            name = template.name,
            parameters = parameters
        )
    }
}

class PersonaNotFoundException(message: String) : RuntimeException(message)
```

---

## 6. Database Migration Example

### Flyway Migration
```sql
-- V100__create_prompt_template_tables.sql

CREATE TABLE prompt_templates (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(255),
    category VARCHAR(100) NOT NULL,
    name VARCHAR(100) NOT NULL,
    version INT NOT NULL DEFAULT 1,
    template_text TEXT NOT NULL,
    parameters JSONB,
    metadata JSONB,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_prompt_category_name ON prompt_templates(category, name);
CREATE INDEX idx_prompt_tenant ON prompt_templates(tenant_id);

CREATE TABLE persona_configurations (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(255),
    persona_key VARCHAR(100) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    description TEXT,
    base_template_id BIGINT NOT NULL,
    language_template_id BIGINT,
    escalation_template_id BIGINT,
    parameters JSONB,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_base_template FOREIGN KEY (base_template_id) REFERENCES prompt_templates(id),
    CONSTRAINT fk_language_template FOREIGN KEY (language_template_id) REFERENCES prompt_templates(id),
    CONSTRAINT fk_escalation_template FOREIGN KEY (escalation_template_id) REFERENCES prompt_templates(id),
    CONSTRAINT uk_persona_tenant_key UNIQUE (tenant_id, persona_key)
);

CREATE INDEX idx_persona_tenant ON persona_configurations(tenant_id);
CREATE INDEX idx_persona_key ON persona_configurations(persona_key);
```

### Seed Data Migration
```sql
-- V101__seed_default_personas.sql

-- Insert base persona template for customer_service
INSERT INTO prompt_templates (category, name, template_text, parameters, active)
VALUES (
    'persona',
    'customer_service_base',
    'You are a helpful customer service representative for this website.

IMPORTANT - When asked "who are you?" or similar identity questions:
- Identify yourself as an AI assistant helping visitors with this website
- Explain that you can answer questions using the website''s knowledge base
- Keep it brief and friendly

Your responses should be:
- CONCISE (2-3 sentences maximum)
- Professional and friendly
- Formatted in Markdown (use **bold**, *italic*, [links](url), etc.)
${includeSources}

FORMATTING RULES (MUST FOLLOW):
- ALWAYS use **bold** for names, key terms, and important words
- NEVER use italic (*) formatting for emphasis
- Keep formatting consistent across all languages',
    '["includeSources", "websiteName"]',
    true
);

-- Insert language instruction template
INSERT INTO prompt_templates (category, name, template_text, parameters, active)
VALUES (
    'system',
    'language_instruction',
    'CRITICAL: Respond in ${languageName} language. The user''s interface is set to ${languageName}.',
    '["language", "languageName"]',
    true
);

-- Create persona configuration
INSERT INTO persona_configurations (
    persona_key,
    display_name,
    description,
    base_template_id,
    language_template_id,
    active
)
VALUES (
    'customer_service',
    'Customer Service',
    'Helpful and friendly customer service representative',
    (SELECT id FROM prompt_templates WHERE name = 'customer_service_base'),
    (SELECT id FROM prompt_templates WHERE name = 'language_instruction'),
    true
);
```

---

## 7. Usage in ConversationalAgentService

### Before (Old Code - 50+ lines)
```kotlin
private val personas = mapOf(
    "customer_service" to """
        You are a helpful customer service representative...
        [50+ lines of hardcoded text]
    """.trimIndent()
)

private fun getPersonaPrompt(persona: String, includeSources: Boolean): String {
    val basePrompt = personas[persona] ?: personas["customer_service"]!!
    // ... complex string manipulation
    return finalPrompt
}
```

### After (New Code - 5 lines)
```kotlin
private fun getPersonaPrompt(
    persona: String,
    language: String?,
    includeSources: Boolean
): String {
    return personaService.renderPersona(
        personaKey = persona,
        parameters = mapOf(
            "language" to (language ?: "en"),
            "languageName" to getLanguageName(language),
            "includeSources" to if (includeSources) {
                "- Include source links when referencing knowledge base facts"
            } else {
                ""
            }
        )
    )
}
```

---

## 8. POM Dependency

```xml
<!-- Apache Commons Text for string substitution -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-text</artifactId>
    <version>1.11.0</version>
</dependency>
```

---

## Benefits Over Handlebars/Velocity

### Apache Commons Text StringSubstitutor:
✅ Lightweight (~50KB)
✅ Simple API
✅ No learning curve
✅ Thread-safe
✅ Handles 90% of use cases
✅ Easy to upgrade to Handlebars later if needed

### When to upgrade to Handlebars:
- Need complex conditionals: `{{#if}}...{{else}}...{{/if}}`
- Need loops: `{{#each}}...{{/each}}`
- Need helpers/custom functions
- Need partials/includes

---

## Next Steps

1. Add `commons-text` dependency
2. Create database migrations (Phase 1)
3. Implement entities and repositories
4. Implement `StringSubstitutorRenderer`
5. Implement `PromptTemplateService` and `PersonaService`
6. Migrate one persona as proof-of-concept
7. Test and validate
