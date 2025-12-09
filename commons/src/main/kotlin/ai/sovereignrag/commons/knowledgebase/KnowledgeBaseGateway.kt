package ai.sovereignrag.commons.knowledgebase

import java.util.UUID

interface KnowledgeBaseGateway {
    fun findById(knowledgeBaseId: String): KnowledgeBaseInfo?

    fun findByOrganization(organizationId: UUID): List<KnowledgeBaseInfo>

    fun existsById(knowledgeBaseId: String): Boolean

    fun updateStats(knowledgeBaseId: String, knowledgeSourceCount: Int, embeddingCount: Int)
}

interface KnowledgeBaseInfo {
    val id: String
    val organizationId: UUID
    val schemaName: String
    val regionCode: String
    val status: KnowledgeBaseStatus
    val oauthClientId: String?
    val apiKeyHash: String?
    val embeddingModelId: String?
}

enum class KnowledgeBaseStatus {
    ACTIVE,
    SUSPENDED,
    DELETED
}

class KnowledgeBaseNotFoundException(message: String) : RuntimeException(message)
