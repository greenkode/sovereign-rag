package ai.sovereignrag.tenant.config

import ai.sovereignrag.commons.tenant.KnowledgeBaseContext
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource
import java.sql.Connection
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.sql.DataSource

private val log = KotlinLogging.logger {}

data class SchemaRoutingKey(
    val organizationId: UUID,
    val schemaName: String
)

abstract class KnowledgeBaseSchemaRouter : AbstractRoutingDataSource() {

    protected val organizationDataSourceCache = ConcurrentHashMap<UUID, DataSource>()

    override fun determineCurrentLookupKey(): Any {
        val context = KnowledgeBaseContext.getContextOrNull()
        return context?.let {
            SchemaRoutingKey(it.organizationId, it.schemaName)
        } ?: "master"
    }

    override fun determineTargetDataSource(): DataSource {
        val context = KnowledgeBaseContext.getContextOrNull()

        if (context == null) {
            log.debug { "No KB context, using master database" }
            return resolvedDefaultDataSource
                ?: throw IllegalStateException("Default datasource not configured")
        }

        return organizationDataSourceCache.computeIfAbsent(context.organizationId) { orgId ->
            createOrganizationDataSource(orgId)
        }
    }

    override fun getConnection(): Connection {
        val connection = super.getConnection()
        val context = KnowledgeBaseContext.getContextOrNull()

        context?.let {
            connection.createStatement().use { stmt ->
                stmt.execute("SET search_path TO ${it.schemaName}, public")
            }
            log.debug { "Set search_path to ${it.schemaName}" }
        }

        return connection
    }

    protected abstract fun createOrganizationDataSource(organizationId: UUID): DataSource

    fun evictOrganizationDataSource(organizationId: UUID) {
        organizationDataSourceCache.remove(organizationId)?.let { ds ->
            log.info { "Evicting datasource for organization: $organizationId" }
            closeDataSource(ds)
        }
    }

    protected open fun closeDataSource(dataSource: DataSource) {
    }

    fun getCachedOrganizations(): Set<UUID> {
        return organizationDataSourceCache.keys.toSet()
    }
}
