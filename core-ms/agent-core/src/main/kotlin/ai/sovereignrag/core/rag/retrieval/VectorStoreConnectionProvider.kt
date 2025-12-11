package ai.sovereignrag.core.rag.retrieval

import ai.sovereignrag.commons.knowledgebase.KnowledgeBaseNotFoundException
import ai.sovereignrag.knowledgebase.knowledgebase.service.KnowledgeBaseRegistryService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

private val log = KotlinLogging.logger {}

data class VectorStoreConnectionInfo(
    val host: String,
    val port: Int,
    val database: String,
    val schema: String,
    val tableName: String,
    val username: String,
    val password: String
)

@Component
class VectorStoreConnectionProvider(
    private val knowledgeBaseRegistryService: KnowledgeBaseRegistryService,
    @Value("\${vector-store.host:\${DB_HOST:localhost}}") private val defaultHost: String,
    @Value("\${vector-store.port:\${DATABASE_PORT:5432}}") private val defaultPort: Int,
    @Value("\${vector-store.database:sovereignrag_master}") private val defaultDatabase: String,
    @Value("\${vector-store.username:\${spring.datasource.main.username:postgres}}") private val defaultUsername: String,
    @Value("\${vector-store.password:\${spring.datasource.main.password:postgres}}") private val defaultPassword: String,
    @Value("\${vector-store.table-prefix:embeddings_}") private val tablePrefix: String,
    @Value("\${vector-store.table-suffix:}") private val tableSuffix: String
) {
    private val connectionCache = ConcurrentHashMap<String, VectorStoreConnectionInfo>()

    fun getConnectionInfo(knowledgeBaseId: String): VectorStoreConnectionInfo {
        return connectionCache.computeIfAbsent(knowledgeBaseId) { kbId ->
            resolveConnectionInfo(kbId)
        }
    }

    private fun resolveConnectionInfo(knowledgeBaseId: String): VectorStoreConnectionInfo {
        val knowledgeBase = runCatching {
            knowledgeBaseRegistryService.getKnowledgeBase(knowledgeBaseId)
        }.getOrElse {
            throw KnowledgeBaseNotFoundException("Knowledge base not found: $knowledgeBaseId")
        }

        val schema = knowledgeBase.schemaName
        val tableName = buildTableName(knowledgeBaseId)

        log.debug { "Resolved vector store connection for KB $knowledgeBaseId: schema=$schema, table=$tableName" }

        return VectorStoreConnectionInfo(
            host = defaultHost,
            port = defaultPort,
            database = defaultDatabase,
            schema = schema,
            tableName = tableName,
            username = defaultUsername,
            password = defaultPassword
        )
    }

    private fun buildTableName(knowledgeBaseId: String): String {
        val sanitizedId = knowledgeBaseId.replace("-", "_").lowercase()
        return "$tablePrefix$sanitizedId$tableSuffix"
    }

    fun evictCache(knowledgeBaseId: String) {
        connectionCache.remove(knowledgeBaseId)
        log.debug { "Evicted connection cache for KB: $knowledgeBaseId" }
    }

    fun clearCache() {
        connectionCache.clear()
        log.info { "Cleared all vector store connection cache" }
    }
}
