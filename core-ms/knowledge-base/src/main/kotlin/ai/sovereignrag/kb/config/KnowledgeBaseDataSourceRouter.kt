package ai.sovereignrag.kb.config

import ai.sovereignrag.commons.knowledgebase.KnowledgeBaseContext
import ai.sovereignrag.commons.knowledgebase.KnowledgeBaseRegistry
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource
import java.util.concurrent.ConcurrentHashMap
import javax.sql.DataSource

private val log = KotlinLogging.logger {}

abstract class KnowledgeBaseDataSourceRouter(
    protected val knowledgeBaseRegistry: KnowledgeBaseRegistry
) : AbstractRoutingDataSource() {

    protected val dataSourceCache = ConcurrentHashMap<String, DataSource>()

    override fun determineCurrentLookupKey(): String {
        val knowledgeBaseId = KnowledgeBaseContext.getKnowledgeBaseIdOrNull()
        return if (knowledgeBaseId == null || knowledgeBaseId == "anonymousUser") "master" else knowledgeBaseId
    }

    override fun determineTargetDataSource(): DataSource {
        val knowledgeBaseId = KnowledgeBaseContext.getKnowledgeBaseIdOrNull()

        if (knowledgeBaseId == null || knowledgeBaseId == "anonymousUser") {
            return resolvedDefaultDataSource
                ?: throw IllegalStateException("Default datasource not configured")
        }

        return dataSourceCache.computeIfAbsent(knowledgeBaseId) { id ->
            createKnowledgeBaseDataSource(id)
        }
    }

    protected abstract fun createKnowledgeBaseDataSource(knowledgeBaseId: String): DataSource

    open fun evictDataSource(knowledgeBaseId: String) {
        dataSourceCache.remove(knowledgeBaseId)?.let { ds ->
            log.info { "Evicting datasource for knowledge base: $knowledgeBaseId" }
            closeDataSource(ds)
        }
    }

    protected open fun closeDataSource(dataSource: DataSource) {
    }

    fun getCachedKnowledgeBases(): Set<String> {
        return dataSourceCache.keys.toSet()
    }
}
