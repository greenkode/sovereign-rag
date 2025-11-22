package ai.sovereignrag.app.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import jakarta.persistence.EntityManagerFactory
import mu.KotlinLogging
import nl.compilot.ai.tenant.config.TenantDataSourceRouter
import nl.compilot.ai.tenant.service.TenantRegistry
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
 * - Chat sessions, messages (nl.compilot.ai.chat.domain)
 * - Content documents (nl.compilot.ai.domain)
 * - Client data (nl.compilot.ai.client.domain)
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
                // IMPORTANT: Clear tenant context before looking up tenant metadata
                // This prevents recursive datasource creation and forces queries to use master DB
                val originalAuth = SecurityContextHolder.getContext().authentication
                try {
                    SecurityContextHolder.clearContext()

                    val tenant = tenantRegistry.getTenant(tenantId)
                    logger.info { "Creating datasource for tenant: $tenantId (database: ${tenant.databaseName})" }

                    val config = HikariConfig().apply {
                        jdbcUrl = "jdbc:postgresql://$dbHost:$dbPort/${tenant.databaseName}"
                        username = dbUsername
                        password = dbPassword

                        // Connection pool settings (per tenant)
                        maximumPoolSize = 10
                        minimumIdle = 2
                        connectionTimeout = 30000
                        idleTimeout = 600000 // 10 minutes
                        maxLifetime = 1800000 // 30 minutes

                        // Pool name for monitoring
                        poolName = "tenant-${tenant.id}"

                        // Performance settings
                        addDataSourceProperty("cachePrepStmts", "true")
                        addDataSourceProperty("prepStmtCacheSize", "250")
                        addDataSourceProperty("prepStmtCacheSqlLimit", "2048")

                        // Validation
                        connectionTestQuery = "SELECT 1"
                        validationTimeout = 5000

                        // Register PostgreSQL types
                        addDataSourceProperty("stringtype", "unspecified")
                    }

                    return HikariDataSource(config)
                } finally {
                    // Restore original authentication
                    if (originalAuth != null) {
                        SecurityContextHolder.getContext().authentication = originalAuth
                    }
                }
            }

            override fun closeDataSource(dataSource: DataSource) {
                if (dataSource is HikariDataSource) {
                    logger.info { "Closing HikariDataSource: ${dataSource.poolName}" }
                    dataSource.close()
                }
            }
        }
    }

    /**
     * Master database datasource
     */
    @Bean
    fun masterDataSource(
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

            // Master pool settings from application.yml
            maximumPoolSize = maxPoolSize
            minimumIdle = minIdle
            this.connectionTimeout = connectionTimeout
            this.idleTimeout = idleTimeout
            this.maxLifetime = maxLifetime

            poolName = "master-pool"

            // Set search path to master schema
            connectionInitSql = "SET search_path TO master, public"

            // Performance settings
            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")

            // PostgreSQL types
            addDataSourceProperty("stringtype", "unspecified")
        }

        return HikariDataSource(config)
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
                "nl.compilot.ai.chat.domain",      // ChatSession, ConversationMessage, Escalation
                "nl.compilot.ai.domain",           // ContentDocument, UnansweredQuery, SearchResult
                "nl.compilot.ai.client.domain"     // Client entities
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
