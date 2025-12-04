package ai.sovereignrag.knowledgebase.knowledgebase.domain

import ai.sovereignrag.commons.knowledgebase.KnowledgeBaseInfo
import ai.sovereignrag.commons.knowledgebase.KnowledgeBaseStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.io.Serializable
import java.time.Instant
import java.util.UUID

@Entity
data class KnowledgeBase(
    @Id
    @Column(name = "id", nullable = false, length = 255)
    override val id: String,

    @Column(name = "name", nullable = false, length = 255)
    val name: String,

    @Column(name = "description")
    val description: String? = null,

    @Column(name = "organization_id", nullable = false)
    override val organizationId: UUID,

    @Column(name = "schema_name", nullable = false, length = 255, unique = true)
    override val schemaName: String,

    @Column(name = "oauth_client_id", length = 100)
    override val oauthClientId: String? = null,

    @Column(name = "api_key_hash", length = 512)
    override var apiKeyHash: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    override val status: KnowledgeBaseStatus = KnowledgeBaseStatus.ACTIVE,

    @Column(name = "max_documents", nullable = false)
    val maxDocuments: Int = 10000,

    @Column(name = "max_embeddings", nullable = false)
    val maxEmbeddings: Int = 50000,

    @Column(name = "max_requests_per_day", nullable = false)
    val maxRequestsPerDay: Int = 10000,

    @Column(name = "contact_email", length = 500)
    val contactEmail: String? = null,

    @Column(name = "contact_name", length = 500)
    val contactName: String? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "features", columnDefinition = "jsonb")
    val features: Map<String, Any> = emptyMap(),

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "settings", columnDefinition = "jsonb")
    val settings: Map<String, Any> = emptyMap(),

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "last_modified_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @Column(name = "last_active_at")
    var lastActiveAt: Instant? = null,

    @Column(name = "deleted_at")
    val deletedAt: Instant? = null
) : KnowledgeBaseInfo, Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}
