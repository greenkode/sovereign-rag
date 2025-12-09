package ai.sovereignrag.commons.knowledgesource

import ai.sovereignrag.commons.embedding.SourceType
import java.time.Instant
import java.util.UUID

interface KnowledgeSourceGateway {
    fun create(knowledgeBaseId: UUID, request: CreateKnowledgeSourceRequest): KnowledgeSourceInfo

    fun update(knowledgeBaseId: UUID, sourceId: UUID, request: UpdateKnowledgeSourceRequest): KnowledgeSourceInfo

    fun updateStatus(knowledgeBaseId: UUID, sourceId: UUID, status: KnowledgeSourceStatus, errorMessage: String? = null)

    fun updateEmbeddingStats(knowledgeBaseId: UUID, sourceId: UUID, chunkCount: Int, embeddingCount: Int)

    fun findById(knowledgeBaseId: UUID, sourceId: UUID): KnowledgeSourceInfo?

    fun findByKnowledgeBase(knowledgeBaseId: UUID, page: Int = 0, size: Int = 50): KnowledgeSourcePage

    fun findByStatus(knowledgeBaseId: UUID, status: KnowledgeSourceStatus): List<KnowledgeSourceInfo>

    fun delete(knowledgeBaseId: UUID, sourceId: UUID)

    fun deleteByKnowledgeBase(knowledgeBaseId: UUID)

    fun countByKnowledgeBase(knowledgeBaseId: UUID): Long

    fun countByStatus(knowledgeBaseId: UUID, status: KnowledgeSourceStatus): Long
}

data class CreateKnowledgeSourceRequest(
    val sourceType: SourceType,
    val fileName: String?,
    val sourceUrl: String?,
    val title: String?,
    val mimeType: String?,
    val fileSize: Long?,
    val s3Key: String?,
    val ingestionJobId: UUID?,
    val metadata: Map<String, Any> = emptyMap()
)

data class UpdateKnowledgeSourceRequest(
    val title: String?,
    val metadata: Map<String, Any>?
)

interface KnowledgeSourceInfo {
    val id: UUID
    val knowledgeBaseId: UUID
    val sourceType: SourceType
    val fileName: String?
    val sourceUrl: String?
    val title: String?
    val mimeType: String?
    val fileSize: Long?
    val s3Key: String?
    val status: KnowledgeSourceStatus
    val errorMessage: String?
    val chunkCount: Int
    val embeddingCount: Int
    val ingestionJobId: UUID?
    val metadata: Map<String, Any>
    val createdAt: Instant
    val updatedAt: Instant
    val processedAt: Instant?
}

data class KnowledgeSourcePage(
    val content: List<KnowledgeSourceInfo>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int
)

enum class KnowledgeSourceStatus {
    PENDING,
    PROCESSING,
    EMBEDDING,
    READY,
    FAILED,
    DELETED
}

class KnowledgeSourceNotFoundException(message: String) : RuntimeException(message)
