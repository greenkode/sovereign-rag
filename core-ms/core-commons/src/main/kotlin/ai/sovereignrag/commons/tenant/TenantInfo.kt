package ai.sovereignrag.commons.tenant

interface KnowledgeBaseInfo {
    val id: String
    val organizationId: java.util.UUID
    val schemaName: String
    val status: KnowledgeBaseStatus
    val oauthClientId: String?
}

enum class KnowledgeBaseStatus {
    ACTIVE,
    SUSPENDED,
    DELETED
}

@Deprecated("Use KnowledgeBaseInfo instead", ReplaceWith("KnowledgeBaseInfo"))
typealias TenantInfo = KnowledgeBaseInfo

@Deprecated("Use KnowledgeBaseStatus instead", ReplaceWith("KnowledgeBaseStatus"))
typealias TenantStatus = KnowledgeBaseStatus
