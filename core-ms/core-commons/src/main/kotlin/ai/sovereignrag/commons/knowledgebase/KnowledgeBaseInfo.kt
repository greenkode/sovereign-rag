package ai.sovereignrag.commons.knowledgebase

import java.util.UUID

interface KnowledgeBaseInfo {
    val id: String
    val organizationId: UUID
    val schemaName: String
    val regionCode: String
    val status: KnowledgeBaseStatus
    val oauthClientId: String?
    val apiKeyHash: String?
}

enum class KnowledgeBaseStatus {
    ACTIVE,
    SUSPENDED,
    DELETED
}
