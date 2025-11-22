package ai.sovereignrag.core.prompt.domain

import jakarta.persistence.*
import java.io.Serializable
import java.time.Instant

/**
 * Persona configuration entity
 *
 * Assembles complete personas from multiple reusable templates.
 * Links base template + optional language/escalation templates.
 */
@Entity
@Table(
    name = "persona_configurations",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_persona_tenant_key",
            columnNames = ["tenant_id", "persona_key"]
        )
    ],
    indexes = [
        Index(name = "idx_persona_tenant", columnList = "tenant_id"),
        Index(name = "idx_persona_key", columnList = "persona_key"),
        Index(name = "idx_persona_active", columnList = "active")
    ]
)
data class PersonaConfiguration(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    /**
     * Tenant ID for tenant-specific personas
     * NULL = global persona available to all tenants
     */
    @Column(name = "tenant_id", length = 255)
    val tenantId: String? = null,

    /**
     * Unique persona identifier
     * Examples: customer_service, professional, casual
     */
    @Column(name = "persona_key", nullable = false, length = 100)
    val personaKey: String,

    /**
     * Human-readable display name
     */
    @Column(name = "display_name", nullable = false, length = 255)
    val displayName: String,

    /**
     * Description of this persona
     */
    @Column(columnDefinition = "TEXT")
    val description: String? = null,

    /**
     * FK to primary persona prompt template
     */
    @Column(name = "base_template_id", nullable = false)
    val baseTemplateId: Long,

    /**
     * FK to optional language instruction template
     */
    @Column(name = "language_template_id")
    val languageTemplateId: Long? = null,

    /**
     * FK to optional escalation messaging template
     */
    @Column(name = "escalation_template_id")
    val escalationTemplateId: Long? = null,

    /**
     * Default parameter values as JSON
     * Example: {"role": "customer service representative", "tone": "friendly"}
     */
    @Column(columnDefinition = "JSONB")
    val parameters: String? = null,

    /**
     * Whether this persona is currently active
     */
    @Column(nullable = false)
    val active: Boolean = true,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
) : Serializable {

    @PreUpdate
    fun onUpdate() {
        updatedAt = Instant.now()
    }
}
