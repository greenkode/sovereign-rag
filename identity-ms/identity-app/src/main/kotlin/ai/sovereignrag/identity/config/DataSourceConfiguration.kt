package ai.sovereignrag.identity.config

import ai.sovereignrag.commons.datasource.ReadWriteRoutingDataSource
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import javax.sql.DataSource

private val log = KotlinLogging.logger {}

@Configuration
class DataSourceConfiguration {

    @Bean
    fun primaryDataSource(
        @Value("\${spring.datasource.url}") url: String,
        @Value("\${spring.datasource.username}") username: String,
        @Value("\${spring.datasource.password}") password: String,
        @Value("\${spring.datasource.hikari.maximum-pool-size:20}") maxPoolSize: Int,
        @Value("\${spring.datasource.hikari.minimum-idle:5}") minIdle: Int,
        @Value("\${spring.datasource.hikari.connectionTimeout:20000}") connectionTimeout: Long,
        @Value("\${spring.datasource.hikari.idleTimeout:300000}") idleTimeout: Long,
        @Value("\${spring.datasource.hikari.maxLifetime:1200000}") maxLifetime: Long
    ): DataSource {
        val config = HikariConfig().apply {
            jdbcUrl = url
            this.username = username
            this.password = password

            maximumPoolSize = maxPoolSize
            minimumIdle = minIdle
            this.connectionTimeout = connectionTimeout
            this.idleTimeout = idleTimeout
            this.maxLifetime = maxLifetime

            poolName = "identity-primary-pool"
            schema = "identity"
            connectionInitSql = "SET search_path TO identity, public"

            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
            addDataSourceProperty("useServerPrepStmts", "true")
            addDataSourceProperty("useLocalSessionState", "true")
            addDataSourceProperty("rewriteBatchedStatements", "true")
            addDataSourceProperty("cacheResultSetMetadata", "true")
            addDataSourceProperty("cacheServerConfiguration", "true")
            addDataSourceProperty("elideSetAutoCommits", "true")
            addDataSourceProperty("maintainTimeStats", "false")
        }

        return HikariDataSource(config)
    }

    @Bean
    fun readReplicaDataSource(
        @Value("\${spring.datasource.read-replica.url:\${spring.datasource.url}}") url: String,
        @Value("\${spring.datasource.username}") username: String,
        @Value("\${spring.datasource.password}") password: String,
        @Value("\${spring.datasource.read-replica.hikari.maximum-pool-size:15}") maxPoolSize: Int,
        @Value("\${spring.datasource.read-replica.hikari.minimum-idle:3}") minIdle: Int,
        @Value("\${spring.datasource.hikari.connectionTimeout:20000}") connectionTimeout: Long,
        @Value("\${spring.datasource.hikari.idleTimeout:300000}") idleTimeout: Long,
        @Value("\${spring.datasource.hikari.maxLifetime:1200000}") maxLifetime: Long
    ): DataSource {
        val config = HikariConfig().apply {
            jdbcUrl = url
            this.username = username
            this.password = password

            maximumPoolSize = maxPoolSize
            minimumIdle = minIdle
            this.connectionTimeout = connectionTimeout
            this.idleTimeout = idleTimeout
            this.maxLifetime = maxLifetime

            poolName = "identity-replica-pool"
            schema = "identity"
            connectionInitSql = "SET search_path TO identity, public"

            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
            addDataSourceProperty("useServerPrepStmts", "true")
            addDataSourceProperty("useLocalSessionState", "true")
            addDataSourceProperty("rewriteBatchedStatements", "true")
            addDataSourceProperty("cacheResultSetMetadata", "true")
            addDataSourceProperty("cacheServerConfiguration", "true")
            addDataSourceProperty("elideSetAutoCommits", "true")
            addDataSourceProperty("maintainTimeStats", "false")
        }

        return HikariDataSource(config)
    }

    @Bean
    @Primary
    fun dataSource(
        @Qualifier("primaryDataSource") primaryDataSource: DataSource,
        @Qualifier("readReplicaDataSource") readReplicaDataSource: DataSource
    ): DataSource {
        val routingDataSource = ReadWriteRoutingDataSource()
        val targetDataSources = mapOf<Any, Any>(
            ReadWriteRoutingDataSource.WRITE_DATASOURCE to primaryDataSource,
            ReadWriteRoutingDataSource.READ_DATASOURCE to readReplicaDataSource
        )
        routingDataSource.setTargetDataSources(targetDataSources)
        routingDataSource.setDefaultTargetDataSource(primaryDataSource)
        routingDataSource.afterPropertiesSet()

        log.info { "Identity database routing configured: writes -> identity-primary-pool, reads -> identity-replica-pool" }
        return routingDataSource
    }
}
