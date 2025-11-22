package ai.sovereignrag.core.content.store

import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.store.embedding.EmbeddingStore
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore
import mu.KotlinLogging
import ai.sovereignrag.commons.tenant.TenantContext
import ai.sovereignrag.commons.tenant.TenantRegistry
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * Factory for creating tenant-specific PgVectorEmbeddingStore instances
 * Uses LangChain4j's built-in PgVectorEmbeddingStore with automatic schema management
 */
@Component
class TenantAwarePgVectorStoreFactory(
    private val tenantRegistry: TenantRegistry,
    @Value("\${spring.datasource.host:localhost}") private val dbHost: String,
    @Value("\${spring.datasource.port:5432}") private val dbPort: Int,
    @Value("\${spring.datasource.username}") private val dbUsername: String,
    @Value("\${spring.datasource.password}") private val dbPassword: String,
    @Value("\${sovereignrag.embedding.dimension:1024}") private val embeddingDimension: Int,
    @Value("\${sovereignrag.embedding.table:langchain4j_embeddings}") private val tableName: String
) {

    // Cache of tenant embedding stores to avoid recreating connections
    private val storeCache = mutableMapOf<String, EmbeddingStore<TextSegment>>()

    /**
     * Get or create embedding store for current tenant
     */
    fun getEmbeddingStore(): EmbeddingStore<TextSegment> {
        val tenantId = TenantContext.getCurrentTenant()

        return storeCache.getOrPut(tenantId) {
            createEmbeddingStore(tenantId)
        }
    }

    /**
     * Get or create embedding store for specific tenant
     */
    fun getEmbeddingStoreForTenant(tenantId: String): EmbeddingStore<TextSegment> {
        return storeCache.getOrPut(tenantId) {
            createEmbeddingStore(tenantId)
        }
    }

    /**
     * Clear cache for a specific tenant (useful after tenant deletion)
     */
    fun clearCache(tenantId: String) {
        storeCache.remove(tenantId)
    }

    /**
     * Clear all cached stores
     */
    fun clearAllCaches() {
        storeCache.clear()
    }

    /**
     * Create a new PgVectorEmbeddingStore for the given tenant
     */
    private fun createEmbeddingStore(tenantId: String): EmbeddingStore<TextSegment> {
        logger.info { "Creating PgVectorEmbeddingStore for tenant: $tenantId" }

        val tenant = tenantRegistry.getTenant(tenantId)
        val databaseName = tenant.databaseName

        val store = PgVectorEmbeddingStore.builder()
            .host(dbHost)
            .port(dbPort)
            .database(databaseName)
            .user(dbUsername)
            .password(dbPassword)
            .table(tableName)
            .dimension(embeddingDimension)
            .createTable(true)  // Automatically create table if it doesn't exist
            .dropTableFirst(false)  // Don't drop existing data
            .build()

        logger.info { "Created PgVectorEmbeddingStore for tenant $tenantId (database: $databaseName, table: $tableName, dimension: $embeddingDimension)" }

        return store
    }
}
