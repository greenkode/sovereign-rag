package ai.sovereignrag.app.config

import ai.sovereignrag.commons.regional.RegionDatabaseConfig
import ai.sovereignrag.commons.regional.RegionNotConfiguredException
import ai.sovereignrag.commons.regional.RegionalDataSourceRegistry
import ai.sovereignrag.commons.regional.RegionalDatabaseProperties
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PreDestroy
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.ConcurrentHashMap
import javax.sql.DataSource

private val log = KotlinLogging.logger {}

@Configuration
@EnableConfigurationProperties(RegionalDatabaseProperties::class)
class RegionalDataSourceConfiguration(
    private val properties: RegionalDatabaseProperties
) {

    private val primaryDataSources = ConcurrentHashMap<String, HikariDataSource>()
    private val replicaDataSources = ConcurrentHashMap<String, HikariDataSource>()

    @Bean
    fun regionalDataSourceRegistry(): RegionalDataSourceRegistry {
        log.info { "Initializing regional data sources for regions: ${properties.regions.keys}" }

        properties.regions.forEach { (regionCode, config) ->
            initializeRegionDataSources(regionCode, config)
        }

        return object : RegionalDataSourceRegistry {
            override fun getDataSource(regionCode: String): DataSource {
                return primaryDataSources[regionCode]
                    ?: throw RegionNotConfiguredException(regionCode)
            }

            override fun getReadReplicaDataSource(regionCode: String): DataSource {
                return replicaDataSources[regionCode]
                    ?: primaryDataSources[regionCode]
                    ?: throw RegionNotConfiguredException(regionCode)
            }

            override fun getAvailableRegions(): Set<String> {
                return primaryDataSources.keys.toSet()
            }

            override fun isRegionAvailable(regionCode: String): Boolean {
                return primaryDataSources.containsKey(regionCode)
            }

            override fun getDatabaseUrl(regionCode: String): String {
                return properties.regions[regionCode]?.url
                    ?: throw RegionNotConfiguredException(regionCode)
            }
        }
    }

    private fun initializeRegionDataSources(regionCode: String, config: RegionDatabaseConfig) {
        log.info { "Creating data source for region: $regionCode" }

        val primaryConfig = HikariConfig().apply {
            jdbcUrl = config.url
            username = config.username
            password = config.password

            maximumPoolSize = config.maxPoolSize
            minimumIdle = config.minIdle
            connectionTimeout = config.connectionTimeout
            idleTimeout = config.idleTimeout
            maxLifetime = config.maxLifetime

            poolName = "regional-$regionCode-primary"

            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
            addDataSourceProperty("stringtype", "unspecified")

            connectionTestQuery = "SELECT 1"
            validationTimeout = 5000
        }

        primaryDataSources[regionCode] = HikariDataSource(primaryConfig)
        log.info { "Created primary data source for region: $regionCode" }

        config.readReplicaUrl?.let { replicaUrl ->
            val replicaConfig = HikariConfig().apply {
                jdbcUrl = replicaUrl
                username = config.username
                password = config.password

                maximumPoolSize = (config.maxPoolSize * 0.8).toInt().coerceAtLeast(2)
                minimumIdle = config.minIdle.coerceAtMost(2)
                connectionTimeout = config.connectionTimeout
                idleTimeout = config.idleTimeout
                maxLifetime = config.maxLifetime

                poolName = "regional-$regionCode-replica"

                addDataSourceProperty("cachePrepStmts", "true")
                addDataSourceProperty("prepStmtCacheSize", "250")
                addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
                addDataSourceProperty("stringtype", "unspecified")

                connectionTestQuery = "SELECT 1"
                validationTimeout = 5000
            }

            replicaDataSources[regionCode] = HikariDataSource(replicaConfig)
            log.info { "Created replica data source for region: $regionCode" }
        }
    }

    @PreDestroy
    fun cleanup() {
        log.info { "Closing regional data sources" }
        primaryDataSources.values.forEach { ds ->
            runCatching { ds.close() }
                .onFailure { e -> log.error(e) { "Error closing primary data source: ${ds.poolName}" } }
        }
        replicaDataSources.values.forEach { ds ->
            runCatching { ds.close() }
                .onFailure { e -> log.error(e) { "Error closing replica data source: ${ds.poolName}" } }
        }
    }
}
