package ai.sovereignrag.knowledgebase.knowledgesource.domain

import ai.sovereignrag.commons.embedding.SourceType
import ai.sovereignrag.commons.knowledgesource.KnowledgeSourceInfo
import ai.sovereignrag.commons.knowledgesource.KnowledgeSourceStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.io.Serializable
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "knowledge_sources")
data class KnowledgeSource(
    @Id
    @Column(name = "id", nullable = false)
    override val id: UUID = UUID.randomUUID(),

    @Column(name = "knowledge_base_id", nullable = false)
    override val knowledgeBaseId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 50)
    override val sourceType: SourceType,

    @Column(name = "file_name", length = 500)
    override val fileName: String? = null,

    @Column(name = "source_url", length = 2000)
    override val sourceUrl: String? = null,

    @Column(name = "title", length = 500)
    override val title: String? = null,

    @Column(name = "mime_type", length = 100)
    override val mimeType: String? = null,

    @Column(name = "file_size")
    override val fileSize: Long? = null,

    @Column(name = "s3_key", length = 500)
    override val s3Key: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    override val status: KnowledgeSourceStatus = KnowledgeSourceStatus.PENDING,

    @Column(name = "error_message", length = 2000)
    override val errorMessage: String? = null,

    @Column(name = "chunk_count", nullable = false)
    override val chunkCount: Int = 0,

    @Column(name = "embedding_count", nullable = false)
    override val embeddingCount: Int = 0,

    @Column(name = "ingestion_job_id")
    override val ingestionJobId: UUID? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    override val metadata: Map<String, Any> = emptyMap(),

    @Column(name = "created_at", nullable = false, updatable = false)
    override val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    override val updatedAt: Instant = Instant.now(),

    @Column(name = "processed_at")
    override val processedAt: Instant? = null
) : KnowledgeSourceInfo, Serializable {

    fun markProcessing(): KnowledgeSource = copy(
        status = KnowledgeSourceStatus.PROCESSING,
        updatedAt = Instant.now()
    )

    fun markEmbedding(chunkCount: Int): KnowledgeSource = copy(
        status = KnowledgeSourceStatus.EMBEDDING,
        chunkCount = chunkCount,
        updatedAt = Instant.now()
    )

    fun markReady(embeddingCount: Int): KnowledgeSource = copy(
        status = KnowledgeSourceStatus.READY,
        embeddingCount = embeddingCount,
        processedAt = Instant.now(),
        updatedAt = Instant.now()
    )

    fun markFailed(errorMessage: String): KnowledgeSource = copy(
        status = KnowledgeSourceStatus.FAILED,
        errorMessage = errorMessage,
        updatedAt = Instant.now()
    )

    fun markDeleted(): KnowledgeSource = copy(
        status = KnowledgeSourceStatus.DELETED,
        updatedAt = Instant.now()
    )

    fun updateStats(chunkCount: Int, embeddingCount: Int): KnowledgeSource = copy(
        chunkCount = chunkCount,
        embeddingCount = embeddingCount,
        updatedAt = Instant.now()
    )

    companion object {
        private const val serialVersionUID = 1L
    }
}
