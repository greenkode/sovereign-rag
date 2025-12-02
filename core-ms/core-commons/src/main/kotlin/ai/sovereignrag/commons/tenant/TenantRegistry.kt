package ai.sovereignrag.commons.tenant

import java.util.UUID

interface KnowledgeBaseRegistry {
    fun getKnowledgeBase(knowledgeBaseId: String): KnowledgeBaseInfo
    fun getKnowledgeBasesByOrganization(organizationId: UUID): List<KnowledgeBaseInfo>
    fun updateLastActive(knowledgeBaseId: String)
}

class KnowledgeBaseNotFoundException(message: String) : RuntimeException(message)

@Deprecated("Use KnowledgeBaseRegistry instead", ReplaceWith("KnowledgeBaseRegistry"))
typealias TenantRegistry = KnowledgeBaseRegistry

@Deprecated("Use KnowledgeBaseNotFoundException instead", ReplaceWith("KnowledgeBaseNotFoundException"))
typealias TenantNotFoundException = KnowledgeBaseNotFoundException
