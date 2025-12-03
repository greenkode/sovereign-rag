package ai.sovereignrag.commons.knowledgebase

import java.util.UUID

interface KnowledgeBaseRegistry {
    fun getKnowledgeBase(knowledgeBaseId: String): KnowledgeBaseInfo
    fun getKnowledgeBasesByOrganization(organizationId: UUID): List<KnowledgeBaseInfo>
    fun validateApiKey(knowledgeBaseId: String, apiKey: String): KnowledgeBaseInfo?
    fun updateLastActive(knowledgeBaseId: String)
}

class KnowledgeBaseNotFoundException(message: String) : RuntimeException(message)
