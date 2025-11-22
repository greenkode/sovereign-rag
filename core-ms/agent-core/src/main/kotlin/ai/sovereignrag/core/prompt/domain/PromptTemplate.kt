package ai.sovereignrag.core.prompt.domain

import jakarta.persistence.*
import java.io.Serializable
import java.time.Instant

/**
 * Prompt template entity with parameter placeholders
 *
 * Stores template text with ${parameter} placeholders for runtime substitution.
 * Supports global templates (tenant_id = null) and tenant-specific overrides.
 */
@Entity
@Table(
    name = "prompt_templates",
    indexes = [
        Index(name = "idx_prompt_category_name", columnList = "category,name"),
        Index(name = "idx_prompt_tenant", columnList = "tenant_id"),
        Index(name = "idx_prompt_active", columnList = "active"),
        Index(name = "idx_prompt_category", columnList = "category")
    ]
)
data class PromptTemplate(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    /**
     * Tenant ID for tenant-specific templates
     * NULL = global template available to all tenants
     */
    @Column(name = "tenant_id", length = 255)
    val tenantId: String? = null,

    /**
     * Template category: persona, system, or instruction
     */
    @Column(nullable = false, length = 100)
    val category: String,

    /**
     * Template name: customer_service_base, language_instruction, etc.
     */
    @Column(nullable = false, length = 100)
    val name: String,

    /**
     * Version number for A/B testing and rollback
     */
    @Column(nullable = false)
    val version: Int = 1,

    /**
     * Template text with ${parameter} placeholders
     * Uses Apache Commons Text StringSubstitutor syntax
     */
    @Column(name = "template_text", nullable = false, columnDefinition = "TEXT")
    val templateText: String,

    /**
     * JSON array of parameter names
     * Example: ["role", "tone", "maxSentences"]
     */
    @Column(columnDefinition = "JSONB")
    val parameters: String? = null,

    /**
     * Additional metadata (tags, description, etc.)
     * Stored as JSON for flexibility
     */
    @Column(columnDefinition = "JSONB")
    val metadata: String? = null,

    /**
     * Whether this template is currently active
     */
    @Column(nullable = false)
    val active: Boolean = true,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) : Serializable {

    companion object {
        // Category constants
        const val CATEGORY_PERSONA = "persona"
        const val CATEGORY_SYSTEM = "system"
        const val CATEGORY_INSTRUCTION = "instruction"
    }

    @PreUpdate
    fun onUpdate() {
        updatedAt = Instant.now()
    }
}
