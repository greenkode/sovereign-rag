package ai.sovereignrag.knowledgebase.knowledgesource.dto

import ai.sovereignrag.commons.embedding.SourceType
import ai.sovereignrag.commons.knowledgesource.KnowledgeSourceStatus
import java.time.Instant
import java.util.UUID

data class KnowledgeSourceDto(
    val id: UUID,
    val knowledgeBaseId: UUID,
    val sourceType: SourceType,
    val fileName: String?,
    val sourceUrl: String?,
    val title: String?,
    val mimeType: String?,
    val fileSize: Long?,
    val status: KnowledgeSourceStatus,
    val errorMessage: String?,
    val chunkCount: Int,
    val embeddingCount: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
    val processedAt: Instant?
)

data class KnowledgeSourceSummaryDto(
    val id: UUID,
    val sourceType: SourceType,
    val fileName: String?,
    val sourceUrl: String?,
    val title: String?,
    val fileSize: Long?,
    val status: KnowledgeSourceStatus,
    val chunkCount: Int,
    val embeddingCount: Int,
    val createdAt: Instant,
    val processedAt: Instant?
)
