package ai.sovereignrag.knowledgebase.knowledgebase.domain

import ai.sovereignrag.commons.knowledgebase.KnowledgeBaseInfo
import ai.sovereignrag.commons.knowledgebase.KnowledgeBaseStatus
import ai.sovereignrag.knowledgebase.knowledgebase.dto.KnowledgeBaseDto
import ai.sovereignrag.knowledgebase.knowledgebase.dto.KnowledgeBaseStats
import ai.sovereignrag.knowledgebase.knowledgebase.dto.KnowledgeBaseSummaryDto
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

    @Column(name = "region_code", nullable = false, length = 20)
    override val regionCode: String = "eu-west",

    @Column(name = "oauth_client_id", length = 100)
    override val oauthClientId: String? = null,

    @Column(name = "api_key_hash", length = 512)
    override var apiKeyHash: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    override val status: KnowledgeBaseStatus = KnowledgeBaseStatus.ACTIVE,

    @Column(name = "max_knowledge_sources", nullable = false)
    val maxKnowledgeSources: Int = 10000,

    @Column(name = "max_embeddings", nullable = false)
    val maxEmbeddings: Int = 50000,

    @Column(name = "max_requests_per_day", nullable = false)
    val maxRequestsPerDay: Int = 10000,

    @Column(name = "contact_email", length = 500)
    val contactEmail: String? = null,

    @Column(name = "contact_name", length = 500)
    val contactName: String? = null,

    @Column(name = "embedding_model_id", length = 100)
    override val embeddingModelId: String? = null,

    @Column(name = "llm_model_id", length = 100)
    override val llmModelId: String? = null,

    @Column(name = "requires_encryption", nullable = false)
    override val requiresEncryption: Boolean = false,

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

    override val systemPrompt: String?
        get() = settings["systemPrompt"] as? String

    override val maxRetrievalResults: Int
        get() = (settings["maxRetrievalResults"] as? Number)?.toInt() ?: 5

    override val minSimilarityScore: Double
        get() = (settings["minSimilarityScore"] as? Number)?.toDouble() ?: 0.7

    override val maxHistoryMessages: Int
        get() = (settings["maxHistoryMessages"] as? Number)?.toInt() ?: 20

    override val enableRemiEvaluation: Boolean
        get() = settings["enableRemiEvaluation"] as? Boolean ?: false

    companion object {
        private const val serialVersionUID = 1L
    }
}
