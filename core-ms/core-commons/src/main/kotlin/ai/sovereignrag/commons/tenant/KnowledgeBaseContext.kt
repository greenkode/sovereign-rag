package ai.sovereignrag.commons.tenant

import java.util.UUID

data class KnowledgeBaseContextData(
    val knowledgeBaseId: String,
    val organizationId: UUID,
    val schemaName: String
)

object KnowledgeBaseContext {
    private val contextHolder = ThreadLocal<KnowledgeBaseContextData?>()

    fun setContext(data: KnowledgeBaseContextData) {
        contextHolder.set(data)
    }

    fun getContext(): KnowledgeBaseContextData {
        return contextHolder.get()
            ?: throw KnowledgeBaseContextException("No knowledge base context set")
    }

    fun getContextOrNull(): KnowledgeBaseContextData? {
        return contextHolder.get()
    }

    fun getKnowledgeBaseId(): String {
        return getContext().knowledgeBaseId
    }

    fun getKnowledgeBaseIdOrNull(): String? {
        return contextHolder.get()?.knowledgeBaseId
    }

    fun getOrganizationId(): UUID {
        return getContext().organizationId
    }

    fun getOrganizationIdOrNull(): UUID? {
        return contextHolder.get()?.organizationId
    }

    fun getSchemaName(): String {
        return getContext().schemaName
    }

    fun getSchemaNameOrNull(): String? {
        return contextHolder.get()?.schemaName
    }

    fun clear() {
        contextHolder.remove()
    }
}

class KnowledgeBaseContextException(message: String) : RuntimeException(message)
