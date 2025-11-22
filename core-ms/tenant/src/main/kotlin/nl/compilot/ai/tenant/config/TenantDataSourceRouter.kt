package nl.compilot.ai.tenant.config

import mu.KotlinLogging
import nl.compilot.ai.commons.tenant.TenantContext
import nl.compilot.ai.tenant.service.TenantRegistry
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource
import java.util.concurrent.ConcurrentHashMap
import javax.sql.DataSource

private val logger = KotlinLogging.logger {}

/**
 * Dynamic DataSource router that switches between tenant databases
 * based on the current TenantContext
 *
 * Each tenant gets its own connection pool to their isolated database
 *
 * NOTE: This class provides the routing logic. The actual DataSource creation
 * is handled by the app module's configuration that has access to HikariCP.
 */
abstract class TenantDataSourceRouter(
    protected val tenantRegistry: TenantRegistry
) : AbstractRoutingDataSource() {

    protected val dataSourceCache = ConcurrentHashMap<String, DataSource>()

    override fun determineCurrentLookupKey(): String {
        val tenant = TenantContext.getCurrentTenantOrNull()
        return if (tenant == null || tenant == "anonymousUser") "master" else tenant
    }

    override fun determineTargetDataSource(): DataSource {
        val tenantId = TenantContext.getCurrentTenantOrNull()

        // If no tenant context or anonymous user, use master database
        if (tenantId == null || tenantId == "anonymousUser") {
            return resolvedDefaultDataSource
                ?: throw IllegalStateException("Default datasource not configured")
        }

        // Get or create datasource for this tenant
        return dataSourceCache.computeIfAbsent(tenantId) { tid ->
            createTenantDataSource(tid)
        }
    }

    /**
     * Create a DataSource for a specific tenant
     * Must be implemented by the app module with access to HikariCP
     */
    protected abstract fun createTenantDataSource(tenantId: String): DataSource

    /**
     * Remove datasource from cache (useful for tenant deletion/maintenance)
     */
    open fun evictDataSource(tenantId: String) {
        dataSourceCache.remove(tenantId)?.let { ds ->
            logger.info { "Evicting datasource for tenant: $tenantId" }
            // Closing logic should be implemented in app module
            closeDataSource(ds)
        }
    }

    /**
     * Close a DataSource
     * Should be overridden in app module to properly close HikariDataSource
     */
    protected open fun closeDataSource(dataSource: DataSource) {
        // Default: no-op, override in app module
    }

    /**
     * Get all cached tenant IDs (for monitoring)
     */
    fun getCachedTenants(): Set<String> {
        return dataSourceCache.keys.toSet()
    }
}
