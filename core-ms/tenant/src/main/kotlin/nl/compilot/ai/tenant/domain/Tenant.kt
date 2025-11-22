package nl.compilot.ai.tenant.domain

import jakarta.persistence.*
import nl.compilot.ai.commons.tenant.TenantInfo
import nl.compilot.ai.commons.tenant.TenantStatus
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.io.Serializable
import java.time.Instant

/**
 * Tenant JPA entity stored in master.tenants table
 * Represents a WordPress site using Compilot AI
 */
@Entity
@Table(name = "tenants", schema = "master")
data class Tenant(
    @Id
    @Column(name = "id", nullable = false, length = 255)
    override val id: String,

    @Column(name = "name", nullable = false, length = 255)
    val name: String,

    @Column(name = "database_name", nullable = false, length = 255, unique = true)
    override val databaseName: String,

    @Column(name = "api_key_hash", nullable = false, length = 512)
    override var apiKeyHash: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    override val status: TenantStatus = TenantStatus.ACTIVE,

    // Limits and quotas
    @Column(name = "max_documents", nullable = false)
    val maxDocuments: Int = 10000,

    @Column(name = "max_embeddings", nullable = false)
    val maxEmbeddings: Int = 50000,

    @Column(name = "max_requests_per_day", nullable = false)
    val maxRequestsPerDay: Int = 10000,

    // Billing
    @Column(name = "subscription_tier", nullable = false, length = 50)
    val subscriptionTier: String = "free",

    // Contact info
    @Column(name = "contact_email", length = 500)
    val contactEmail: String? = null,

    @Column(name = "contact_name", length = 500)
    val contactName: String? = null,

    // Admin email for API key reset (secure operations)
    @Column(name = "admin_email", length = 255)
    val adminEmail: String? = null,

    // WordPress site info
    @Column(name = "wordpress_url", length = 1000)
    val wordpressUrl: String? = null,

    @Column(name = "wordpress_version", length = 50)
    val wordpressVersion: String? = null,

    @Column(name = "plugin_version", length = 50)
    val pluginVersion: String? = null,

    // Features and settings (JSON stored as maps)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "features", columnDefinition = "jsonb")
    val features: Map<String, Any> = emptyMap(),

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "settings", columnDefinition = "jsonb")
    val settings: Map<String, Any> = emptyMap(),

    // Timestamps
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @Column(name = "last_active_at")
    var lastActiveAt: Instant? = null,

    // Soft delete
    @Column(name = "deleted_at")
    val deletedAt: Instant? = null
) : TenantInfo, Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}
