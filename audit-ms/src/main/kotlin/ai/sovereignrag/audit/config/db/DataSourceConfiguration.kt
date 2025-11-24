package ai.sovereignrag.audit.config.db

import ai.sovereignrag.audit.datasource.ReadWriteRoutingDataSource
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
        @Value("\${spring.datasource.main.url}") url: String,
        @Value("\${spring.datasource.main.username}") username: String,
        @Value("\${spring.datasource.main.password}") password: String,
        @Value("\${spring.datasource.main.driver-class-name}") driverClassName: String,
        @Value("\${spring.datasource.main.hikari.maximum-pool-size}") maximumPoolSize: Int,
        @Value("\${spring.datasource.main.hikari.minimum-idle}") minimumIdle: Int,
        @Value("\${spring.datasource.main.hikari.schema}") schema: String,
        @Value("\${spring.datasource.main.hikari.pool-name}") poolName: String,
        @Value("\${spring.datasource.main.hikari.connectionTimeout}") connectionTimeout: Long,
        @Value("\${spring.datasource.main.hikari.idleTimeout}") idleTimeout: Long,
        @Value("\${spring.datasource.main.hikari.maxLifetime}") maxLifetime: Long,
        @Value("\${spring.datasource.main.hikari.register-mbeans}") registerMbeans: Boolean,
        @Value("\${spring.datasource.main.hikari.data-source-properties.cachePrepStmts}") cachePrepStmts: Boolean,
        @Value("\${spring.datasource.main.hikari.data-source-properties.prepStmtCacheSize}") prepStmtCacheSize: Int,
        @Value("\${spring.datasource.main.hikari.data-source-properties.prepStmtCacheSqlLimit}") prepStmtCacheSqlLimit: Int,
        @Value("\${spring.datasource.main.hikari.data-source-properties.useServerPrepStmts}") useServerPrepStmts: Boolean,
        @Value("\${spring.datasource.main.hikari.data-source-properties.useLocalSessionState}") useLocalSessionState: Boolean,
        @Value("\${spring.datasource.main.hikari.data-source-properties.rewriteBatchedStatements}") rewriteBatchedStatements: Boolean,
        @Value("\${spring.datasource.main.hikari.data-source-properties.cacheResultSetMetadata}") cacheResultSetMetadata: Boolean,
        @Value("\${spring.datasource.main.hikari.data-source-properties.cacheServerConfiguration}") cacheServerConfiguration: Boolean,
        @Value("\${spring.datasource.main.hikari.data-source-properties.elideSetAutoCommits}") elideSetAutoCommits: Boolean,
        @Value("\${spring.datasource.main.hikari.data-source-properties.maintainTimeStats}") maintainTimeStats: Boolean,
        @Value("\${spring.datasource.main.hikari.data-source-properties.application}") applicationName: String
    ): DataSource {
        return createHikariDataSource(
            url, username, password, driverClassName, maximumPoolSize, minimumIdle, schema, "$poolName-primary",
            connectionTimeout, idleTimeout, maxLifetime, registerMbeans,
            cachePrepStmts, prepStmtCacheSize, prepStmtCacheSqlLimit, useServerPrepStmts,
            useLocalSessionState, rewriteBatchedStatements, cacheResultSetMetadata,
            cacheServerConfiguration, elideSetAutoCommits, maintainTimeStats, applicationName
        )
    }

    @Bean
    fun readReplicaDataSource(
        @Value("\${spring.datasource.read-replica.url}") url: String,
        @Value("\${spring.datasource.read-replica.username}") username: String,
        @Value("\${spring.datasource.read-replica.password}") password: String,
        @Value("\${spring.datasource.read-replica.driver-class-name}") driverClassName: String,
        @Value("\${spring.datasource.read-replica.hikari.maximum-pool-size}") maximumPoolSize: Int,
        @Value("\${spring.datasource.read-replica.hikari.minimum-idle}") minimumIdle: Int,
        @Value("\${spring.datasource.read-replica.hikari.schema}") schema: String,
        @Value("\${spring.datasource.read-replica.hikari.pool-name}") poolName: String,
        @Value("\${spring.datasource.read-replica.hikari.connectionTimeout}") connectionTimeout: Long,
        @Value("\${spring.datasource.read-replica.hikari.idleTimeout}") idleTimeout: Long,
        @Value("\${spring.datasource.read-replica.hikari.maxLifetime}") maxLifetime: Long,
        @Value("\${spring.datasource.read-replica.hikari.register-mbeans}") registerMbeans: Boolean,
        @Value("\${spring.datasource.read-replica.hikari.data-source-properties.cachePrepStmts}") cachePrepStmts: Boolean,
        @Value("\${spring.datasource.read-replica.hikari.data-source-properties.prepStmtCacheSize}") prepStmtCacheSize: Int,
        @Value("\${spring.datasource.read-replica.hikari.data-source-properties.prepStmtCacheSqlLimit}") prepStmtCacheSqlLimit: Int,
        @Value("\${spring.datasource.read-replica.hikari.data-source-properties.useServerPrepStmts}") useServerPrepStmts: Boolean,
        @Value("\${spring.datasource.read-replica.hikari.data-source-properties.useLocalSessionState}") useLocalSessionState: Boolean,
        @Value("\${spring.datasource.read-replica.hikari.data-source-properties.rewriteBatchedStatements}") rewriteBatchedStatements: Boolean,
        @Value("\${spring.datasource.read-replica.hikari.data-source-properties.cacheResultSetMetadata}") cacheResultSetMetadata: Boolean,
        @Value("\${spring.datasource.read-replica.hikari.data-source-properties.cacheServerConfiguration}") cacheServerConfiguration: Boolean,
        @Value("\${spring.datasource.read-replica.hikari.data-source-properties.elideSetAutoCommits}") elideSetAutoCommits: Boolean,
        @Value("\${spring.datasource.read-replica.hikari.data-source-properties.maintainTimeStats}") maintainTimeStats: Boolean,
        @Value("\${spring.datasource.read-replica.hikari.data-source-properties.application}") applicationName: String
    ): DataSource {
        return createHikariDataSource(
            url, username, password, driverClassName, maximumPoolSize, minimumIdle, schema, "$poolName-replica",
            connectionTimeout, idleTimeout, maxLifetime, registerMbeans,
            cachePrepStmts, prepStmtCacheSize, prepStmtCacheSqlLimit, useServerPrepStmts,
            useLocalSessionState, rewriteBatchedStatements, cacheResultSetMetadata,
            cacheServerConfiguration, elideSetAutoCommits, maintainTimeStats, applicationName
        )
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

        log.info { "Audit database routing configured: writes -> primary, reads -> replica" }
        return routingDataSource
    }

    private fun createHikariDataSource(
        url: String,
        username: String,
        password: String,
        driverClassName: String,
        maximumPoolSize: Int,
        minimumIdle: Int,
        schema: String,
        poolName: String,
        connectionTimeout: Long,
        idleTimeout: Long,
        maxLifetime: Long,
        registerMbeans: Boolean,
        cachePrepStmts: Boolean,
        prepStmtCacheSize: Int,
        prepStmtCacheSqlLimit: Int,
        useServerPrepStmts: Boolean,
        useLocalSessionState: Boolean,
        rewriteBatchedStatements: Boolean,
        cacheResultSetMetadata: Boolean,
        cacheServerConfiguration: Boolean,
        elideSetAutoCommits: Boolean,
        maintainTimeStats: Boolean,
        applicationName: String
    ): DataSource {
        val config = HikariConfig()

        config.jdbcUrl = url
        config.username = username
        config.password = password
        config.driverClassName = driverClassName

        config.maximumPoolSize = maximumPoolSize
        config.minimumIdle = minimumIdle
        config.schema = schema
        config.poolName = poolName
        config.connectionTimeout = connectionTimeout
        config.idleTimeout = idleTimeout
        config.maxLifetime = maxLifetime
        config.isRegisterMbeans = registerMbeans

        config.addDataSourceProperty("cachePrepStmts", cachePrepStmts.toString())
        config.addDataSourceProperty("prepStmtCacheSize", prepStmtCacheSize.toString())
        config.addDataSourceProperty("prepStmtCacheSqlLimit", prepStmtCacheSqlLimit.toString())
        config.addDataSourceProperty("useServerPrepStmts", useServerPrepStmts.toString())
        config.addDataSourceProperty("useLocalSessionState", useLocalSessionState.toString())
        config.addDataSourceProperty("rewriteBatchedStatements", rewriteBatchedStatements.toString())
        config.addDataSourceProperty("cacheResultSetMetadata", cacheResultSetMetadata.toString())
        config.addDataSourceProperty("cacheServerConfiguration", cacheServerConfiguration.toString())
        config.addDataSourceProperty("elideSetAutoCommits", elideSetAutoCommits.toString())
        config.addDataSourceProperty("maintainTimeStats", maintainTimeStats.toString())
        config.addDataSourceProperty("application", applicationName)

        return HikariDataSource(config)
    }
}
