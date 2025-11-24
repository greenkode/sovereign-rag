package ai.sovereignrag.app.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import jakarta.persistence.EntityManagerFactory
import io.github.oshai.kotlinlogging.KotlinLogging
import ai.sovereignrag.commons.datasource.ReadWriteRoutingDataSource
import ai.sovereignrag.tenant.config.TenantDataSourceRouter
import ai.sovereignrag.tenant.service.TenantRegistry
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Primary
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import javax.sql.DataSource

private val logger = KotlinLogging.logger {}

/**
 * EntityManager configuration for TENANT database entities
 *
 * This EntityManager uses TenantDataSourceRouter for dynamic routing to tenant databases.
 * Handles:
 * - Chat sessions, messages (ai.sovereignrag.chat.domain)
 * - Content documents (ai.sovereignrag.domain)
 * - Client data (ai.sovereignrag.client.domain)
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    basePackages = [
        "ai.sovereignrag.client.repository"
    ],
    entityManagerFactoryRef = "tenantEntityManagerFactory",
    transactionManagerRef = "tenantTransactionManager"
)
class TenantDataSourceConfiguration {

    @Bean
    @Primary
    fun tenantDataSourceRouter(
        @Lazy tenantRegistry: TenantRegistry,
        @Value("\${spring.datasource.username}") dbUsername: String,
        @Value("\${spring.datasource.password}") dbPassword: String,
        @Value("\${spring.datasource.host:localhost}") dbHost: String,
        @Value("\${spring.datasource.port:5432}") dbPort: Int,
        masterDataSource: DataSource
    ): TenantDataSourceRouter {
        return object : TenantDataSourceRouter(tenantRegistry) {

            init {
                // Set the default datasource (master database)
                setDefaultTargetDataSource(masterDataSource)
                // Initialize with empty target datasources (will be populated dynamically)
                setTargetDataSources(mutableMapOf<Any, Any>())
                afterPropertiesSet()
            }

            override fun createTenantDataSource(tenantId: String): DataSource {
                val originalAuth = SecurityContextHolder.getContext().authentication
                try {
                    SecurityContextHolder.clearContext()

                    val tenant = tenantRegistry.getTenant(tenantId)
                    logger.info { "Creating routing datasource for tenant: $tenantId (database: ${tenant.databaseName})" }

                    val primaryConfig = HikariConfig().apply {
                        jdbcUrl = "jdbc:postgresql://$dbHost:$dbPort/${tenant.databaseName}"
                        username = dbUsername
                        password = dbPassword

                        maximumPoolSize = 10
                        minimumIdle = 2
                        connectionTimeout = 30000
                        idleTimeout = 600000
                        maxLifetime = 1800000

                        poolName = "tenant-${tenant.id}-primary"

                        addDataSourceProperty("cachePrepStmts", "true")
                        addDataSourceProperty("prepStmtCacheSize", "250")
                        addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
                        addDataSourceProperty("stringtype", "unspecified")

                        connectionTestQuery = "SELECT 1"
                        validationTimeout = 5000
                    }

                    val replicaConfig = HikariConfig().apply {
                        jdbcUrl = "jdbc:postgresql://$dbHost:$dbPort/${tenant.databaseName}"
                        username = dbUsername
                        password = dbPassword

                        maximumPoolSize = 8
                        minimumIdle = 1
                        connectionTimeout = 30000
                        idleTimeout = 600000
                        maxLifetime = 1800000

                        poolName = "tenant-${tenant.id}-replica"

                        addDataSourceProperty("cachePrepStmts", "true")
                        addDataSourceProperty("prepStmtCacheSize", "250")
                        addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
                        addDataSourceProperty("stringtype", "unspecified")

                        connectionTestQuery = "SELECT 1"
                        validationTimeout = 5000
                    }

                    val primaryDataSource = HikariDataSource(primaryConfig)
                    val replicaDataSource = HikariDataSource(replicaConfig)

                    val routingDataSource = ReadWriteRoutingDataSource()
                    val targetDataSources = mapOf<Any, Any>(
                        ReadWriteRoutingDataSource.WRITE_DATASOURCE to primaryDataSource,
                        ReadWriteRoutingDataSource.READ_DATASOURCE to replicaDataSource
                    )
                    routingDataSource.setTargetDataSources(targetDataSources)
                    routingDataSource.setDefaultTargetDataSource(primaryDataSource)
                    routingDataSource.afterPropertiesSet()

                    logger.info { "Tenant $tenantId routing configured: writes -> tenant-${tenant.id}-primary, reads -> tenant-${tenant.id}-replica" }
                    return routingDataSource
                } finally {
                    if (originalAuth != null) {
                        SecurityContextHolder.getContext().authentication = originalAuth
                    }
                }
            }

            override fun closeDataSource(dataSource: DataSource) {
                when (dataSource) {
                    is ReadWriteRoutingDataSource -> {
                        val resolvedDataSources = dataSource.resolvedDataSources
                        resolvedDataSources?.values?.forEach { ds ->
                            if (ds is HikariDataSource) {
                                logger.info { "Closing HikariDataSource: ${ds.poolName}" }
                                ds.close()
                            }
                        }
                    }
                    is HikariDataSource -> {
                        logger.info { "Closing HikariDataSource: ${dataSource.poolName}" }
                        dataSource.close()
                    }
                }
            }
        }
    }

    @Bean
    fun masterPrimaryDataSource(
        @Value("\${spring.datasource.url}") url: String,
        @Value("\${spring.datasource.username}") username: String,
        @Value("\${spring.datasource.password}") password: String,
        @Value("\${spring.datasource.hikari.maximum-pool-size:20}") maxPoolSize: Int,
        @Value("\${spring.datasource.hikari.minimum-idle:5}") minIdle: Int,
        @Value("\${spring.datasource.hikari.connection-timeout:30000}") connectionTimeout: Long,
        @Value("\${spring.datasource.hikari.idle-timeout:600000}") idleTimeout: Long,
        @Value("\${spring.datasource.hikari.max-lifetime:1800000}") maxLifetime: Long
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

            poolName = "master-primary-pool"
            connectionInitSql = "SET search_path TO master, public"

            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
            addDataSourceProperty("stringtype", "unspecified")
        }

        return HikariDataSource(config)
    }

    @Bean
    fun masterReadReplicaDataSource(
        @Value("\${spring.datasource.read-replica.url:\${spring.datasource.url}}") url: String,
        @Value("\${spring.datasource.username}") username: String,
        @Value("\${spring.datasource.password}") password: String,
        @Value("\${spring.datasource.read-replica.hikari.maximum-pool-size:15}") maxPoolSize: Int,
        @Value("\${spring.datasource.read-replica.hikari.minimum-idle:3}") minIdle: Int,
        @Value("\${spring.datasource.hikari.connection-timeout:30000}") connectionTimeout: Long,
        @Value("\${spring.datasource.hikari.idle-timeout:600000}") idleTimeout: Long,
        @Value("\${spring.datasource.hikari.max-lifetime:1800000}") maxLifetime: Long
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

            poolName = "master-replica-pool"
            connectionInitSql = "SET search_path TO master, public"

            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
            addDataSourceProperty("stringtype", "unspecified")
        }

        return HikariDataSource(config)
    }

    @Bean
    fun masterDataSource(
        @Qualifier("masterPrimaryDataSource") masterPrimaryDataSource: DataSource,
        @Qualifier("masterReadReplicaDataSource") masterReadReplicaDataSource: DataSource
    ): DataSource {
        val routingDataSource = ReadWriteRoutingDataSource()
        val targetDataSources = mapOf<Any, Any>(
            ReadWriteRoutingDataSource.WRITE_DATASOURCE to masterPrimaryDataSource,
            ReadWriteRoutingDataSource.READ_DATASOURCE to masterReadReplicaDataSource
        )
        routingDataSource.setTargetDataSources(targetDataSources)
        routingDataSource.setDefaultTargetDataSource(masterPrimaryDataSource)
        routingDataSource.afterPropertiesSet()

        logger.info { "Master database routing configured: writes -> master-primary-pool, reads -> master-replica-pool" }
        return routingDataSource
    }

    /**
     * EntityManagerFactory for tenant database
     * Uses TenantDataSourceRouter for dynamic routing to tenant databases
     */
    @Bean
    @Primary
    fun tenantEntityManagerFactory(
        builder: EntityManagerFactoryBuilder,
        @Qualifier("tenantDataSourceRouter") tenantDataSourceRouter: DataSource,
        jpaProperties: JpaProperties
    ): LocalContainerEntityManagerFactoryBean {
        return builder
            .dataSource(tenantDataSourceRouter)
            .packages(
                "ai.sovereignrag.chat.domain",      // ChatSession, ConversationMessage, Escalation
                "ai.sovereignrag.domain",           // ContentDocument, UnansweredQuery, SearchResult
                "ai.sovereignrag.client.domain"     // Client entities
            )
            .persistenceUnit("tenant")
            .properties(jpaProperties.properties)
            .build()
    }

    /**
     * Transaction manager for tenant database operations
     * Marked as @Primary so it's the default for most operations
     */
    @Bean(value = ["tenantTransactionManager", "transactionManager"])
    @Primary
    fun tenantTransactionManager(
        @Qualifier("tenantEntityManagerFactory") tenantEntityManagerFactory: EntityManagerFactory
    ): PlatformTransactionManager {
        return JpaTransactionManager(tenantEntityManagerFactory)
    }
}
