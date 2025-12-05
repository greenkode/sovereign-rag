package ai.sovereignrag.knowledgebase.knowledgebase.dto

import ai.sovereignrag.commons.knowledgebase.KnowledgeBaseStatus
import java.time.Instant
import java.util.UUID

data class KnowledgeBaseDto(
    val id: String,
    val name: String,
    val description: String?,
    val organizationId: UUID,
    val status: KnowledgeBaseStatus,
    val knowledgeSourceCount: Int,
    val embeddingCount: Int,
    val queryCount: Int,
    val maxKnowledgeSources: Int,
    val maxEmbeddings: Int,
    val maxRequestsPerDay: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastActiveAt: Instant?
)

data class KnowledgeBaseStats(
    val knowledgeSourceCount: Int = 0,
    val embeddingCount: Int = 0,
    val queryCount: Int = 0
)

data class KnowledgeBaseSummaryDto(
    val id: String,
    val name: String,
    val description: String?,
    val organizationId: UUID,
    val status: KnowledgeBaseStatus,
    val knowledgeSourceCount: Int,
    val lastActiveAt: Instant?,
    val createdAt: Instant
)

data class CreateKnowledgeBaseResult(
    val knowledgeBase: KnowledgeBaseDto,
    val clientId: String,
    val clientSecret: String
)
