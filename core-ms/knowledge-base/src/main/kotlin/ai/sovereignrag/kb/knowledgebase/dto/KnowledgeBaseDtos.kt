package ai.sovereignrag.kb.knowledgebase.dto

import ai.sovereignrag.commons.knowledgebase.KnowledgeBaseStatus
import ai.sovereignrag.kb.knowledgebase.domain.KnowledgeBase
import java.time.Instant
import java.util.UUID

data class KnowledgeBaseDto(
    val id: String,
    val name: String,
    val description: String?,
    val organizationId: UUID,
    val status: KnowledgeBaseStatus,
    val documentCount: Int,
    val embeddingCount: Int,
    val queryCount: Int,
    val maxDocuments: Int,
    val maxEmbeddings: Int,
    val maxRequestsPerDay: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastActiveAt: Instant?
) {
    companion object {
        fun from(kb: KnowledgeBase, stats: KnowledgeBaseStats = KnowledgeBaseStats()): KnowledgeBaseDto {
            return KnowledgeBaseDto(
                id = kb.id,
                name = kb.name,
                description = kb.description,
                organizationId = kb.organizationId,
                status = kb.status,
                documentCount = stats.documentCount,
                embeddingCount = stats.embeddingCount,
                queryCount = stats.queryCount,
                maxDocuments = kb.maxDocuments,
                maxEmbeddings = kb.maxEmbeddings,
                maxRequestsPerDay = kb.maxRequestsPerDay,
                createdAt = kb.createdAt,
                updatedAt = kb.updatedAt,
                lastActiveAt = kb.lastActiveAt
            )
        }
    }
}

data class KnowledgeBaseStats(
    val documentCount: Int = 0,
    val embeddingCount: Int = 0,
    val queryCount: Int = 0
)

data class KnowledgeBaseSummaryDto(
    val id: String,
    val name: String,
    val description: String?,
    val organizationId: UUID,
    val status: KnowledgeBaseStatus,
    val documentCount: Int,
    val lastActiveAt: Instant?,
    val createdAt: Instant
) {
    companion object {
        fun from(kb: KnowledgeBase, documentCount: Int = 0): KnowledgeBaseSummaryDto {
            return KnowledgeBaseSummaryDto(
                id = kb.id,
                name = kb.name,
                description = kb.description,
                organizationId = kb.organizationId,
                status = kb.status,
                documentCount = documentCount,
                lastActiveAt = kb.lastActiveAt,
                createdAt = kb.createdAt
            )
        }
    }
}

data class CreateKnowledgeBaseResult(
    val knowledgeBase: KnowledgeBaseDto,
    val clientId: String,
    val clientSecret: String
)
