package ai.sovereignrag.ingestion.core.config.db

import ai.sovereignrag.commons.regional.RegionNotConfiguredException
import ai.sovereignrag.commons.regional.RegionalDatabaseProperties
import ai.sovereignrag.ingestion.core.gateway.CoreMsKnowledgeBaseGateway
import ai.sovereignrag.ingestion.core.gateway.KnowledgeBaseDatabaseConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PreDestroy
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import javax.sql.DataSource

private val log = KotlinLogging.logger {}

data class KnowledgeBaseConnectionInfo(
    val host: String,
    val port: Int,
    val database: String,
    val schema: String,
    val username: String,
    val password: String
)

@Component
class KnowledgeBaseDataSourceProvider(
    private val knowledgeBaseGateway: CoreMsKnowledgeBaseGateway,
    private val regionalDatabaseProperties: RegionalDatabaseProperties,
    @Value("\${spring.datasource.main.username}") private val defaultUsername: String,
    @Value("\${spring.datasource.main.password}") private val defaultPassword: String,
    @Value("\${DB_HOST:localhost}") private val defaultHost: String,
    @Value("\${DATABASE_PORT:5432}") private val defaultPort: Int
) {

    private val dataSourceCache = ConcurrentHashMap<String, HikariDataSource>()
    private val connectionInfoCache = ConcurrentHashMap<String, KnowledgeBaseConnectionInfo>()

    fun getDataSource(knowledgeBaseId: String): DataSource {
        return dataSourceCache.computeIfAbsent(knowledgeBaseId) { kbId ->
            createDataSource(kbId)
        }
    }

    fun getConnectionInfo(knowledgeBaseId: String): KnowledgeBaseConnectionInfo {
        return connectionInfoCache.computeIfAbsent(knowledgeBaseId) { kbId ->
            resolveConnectionInfo(kbId)
        }
    }

    fun evictDataSource(knowledgeBaseId: String) {
        dataSourceCache.remove(knowledgeBaseId)?.let { ds ->
            log.info { "Evicting datasource for knowledge base: $knowledgeBaseId" }
            runCatching { ds.close() }
                .onFailure { e -> log.error(e) { "Error closing datasource for $knowledgeBaseId" } }
        }
        connectionInfoCache.remove(knowledgeBaseId)
        knowledgeBaseGateway.evictDatabaseConfigCache(knowledgeBaseId)
    }

    private fun createDataSource(knowledgeBaseId: String): HikariDataSource {
        val connectionInfo = getConnectionInfo(knowledgeBaseId)

        log.info { "Creating datasource for knowledge base: $knowledgeBaseId (db: ${connectionInfo.database}, schema: ${connectionInfo.schema})" }

        val config = HikariConfig().apply {
            jdbcUrl = "jdbc:postgresql://${connectionInfo.host}:${connectionInfo.port}/${connectionInfo.database}"
            username = connectionInfo.username
            password = connectionInfo.password
            schema = connectionInfo.schema

            maximumPoolSize = 5
            minimumIdle = 1
            connectionTimeout = 30000
            idleTimeout = 300000
            maxLifetime = 600000

            poolName = "kb-embedding-${knowledgeBaseId.take(8)}"

            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "100")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
            addDataSourceProperty("stringtype", "unspecified")

            connectionTestQuery = "SELECT 1"
            validationTimeout = 5000
        }

        return HikariDataSource(config).also {
            log.info { "Created datasource for knowledge base: $knowledgeBaseId (pool: ${it.poolName})" }
        }
    }

    private fun resolveConnectionInfo(knowledgeBaseId: String): KnowledgeBaseConnectionInfo {
        val dbConfig = knowledgeBaseGateway.getDatabaseConfig(knowledgeBaseId)
        return resolveConnectionFromConfig(dbConfig)
    }

    private fun resolveConnectionFromConfig(config: KnowledgeBaseDatabaseConfig): KnowledgeBaseConnectionInfo {
        val regionCode = config.regionCode
        val regionConfig = regionalDatabaseProperties.regions[regionCode]

        return if (regionConfig != null) {
            val parsedUrl = parseJdbcUrl(regionConfig.url)
            KnowledgeBaseConnectionInfo(
                host = parsedUrl.host,
                port = parsedUrl.port,
                database = config.databaseName,
                schema = config.schemaName,
                username = regionConfig.username,
                password = regionConfig.password
            )
        } else {
            log.warn { "No regional config for region: $regionCode, using default connection" }
            KnowledgeBaseConnectionInfo(
                host = defaultHost,
                port = defaultPort,
                database = config.databaseName,
                schema = config.schemaName,
                username = defaultUsername,
                password = defaultPassword
            )
        }
    }

    private fun parseJdbcUrl(jdbcUrl: String): JdbcUrlParts {
        val regex = Regex("""jdbc:postgresql://([^:/?]+):?(\d+)?/([^?]+).*""")
        val match = regex.find(jdbcUrl)
        return match?.let {
            JdbcUrlParts(
                host = it.groupValues[1],
                port = it.groupValues[2].takeIf { p -> p.isNotEmpty() }?.toInt() ?: 5432,
                database = it.groupValues[3]
            )
        } ?: JdbcUrlParts(defaultHost, defaultPort, "sovereignrag_master")
    }

    fun getCachedKnowledgeBases(): Set<String> {
        return dataSourceCache.keys.toSet()
    }

    @PreDestroy
    fun cleanup() {
        log.info { "Closing ${dataSourceCache.size} knowledge base datasources" }
        dataSourceCache.forEach { (kbId, ds) ->
            runCatching { ds.close() }
                .onFailure { e -> log.error(e) { "Error closing datasource for $kbId" } }
        }
        dataSourceCache.clear()
        connectionInfoCache.clear()
    }

    private data class JdbcUrlParts(
        val host: String,
        val port: Int,
        val database: String
    )
}
