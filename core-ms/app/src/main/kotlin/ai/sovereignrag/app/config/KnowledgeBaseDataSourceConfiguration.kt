package ai.sovereignrag.app.config

import ai.sovereignrag.commons.datasource.ReadWriteRoutingDataSource
import ai.sovereignrag.commons.knowledgebase.KnowledgeBaseRegistry
import ai.sovereignrag.commons.regional.RegionalDataSourceRegistry
import ai.sovereignrag.commons.regional.RegionalDatabaseProperties
import ai.sovereignrag.knowledgebase.config.KnowledgeBaseDataSourceRouter
import ai.sovereignrag.knowledgebase.knowledgebase.service.OrganizationDatabaseRegistry
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.persistence.EntityManagerFactory
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

private val log = KotlinLogging.logger {}

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    basePackages = [
        "ai.sovereignrag.knowledgebase.repository"
    ],
    entityManagerFactoryRef = "knowledgeBaseEntityManagerFactory",
    transactionManagerRef = "knowledgeBaseTransactionManager"
)
class KnowledgeBaseDataSourceConfiguration {

    @Bean
    @Primary
    fun knowledgeBaseDataSourceRouter(
        @Lazy knowledgeBaseRegistry: KnowledgeBaseRegistry,
        @Lazy organizationDatabaseRegistry: OrganizationDatabaseRegistry,
        @Value("\${spring.datasource.username}") dbUsername: String,
        @Value("\${spring.datasource.password}") dbPassword: String,
        @Value("\${spring.datasource.host:localhost}") dbHost: String,
        @Value("\${spring.datasource.port:5432}") dbPort: Int,
        masterDataSource: DataSource,
        regionalDatabaseProperties: RegionalDatabaseProperties
    ): KnowledgeBaseDataSourceRouter {
        return object : KnowledgeBaseDataSourceRouter(knowledgeBaseRegistry) {

            init {
                setDefaultTargetDataSource(masterDataSource)
                setTargetDataSources(mutableMapOf<Any, Any>())
                afterPropertiesSet()
            }

            override fun createKnowledgeBaseDataSource(knowledgeBaseId: String): DataSource {
                val originalAuth = SecurityContextHolder.getContext().authentication
                try {
                    SecurityContextHolder.clearContext()

                    val kb = knowledgeBaseRegistry.getKnowledgeBase(knowledgeBaseId)
                    val regionCode = kb.regionCode
                    val databaseName = organizationDatabaseRegistry.getDatabaseName(kb.organizationId)

                    log.info { "Creating routing datasource for knowledge base: $knowledgeBaseId (schema: ${kb.schemaName}, region: $regionCode, database: $databaseName)" }

                    val regionConfig = regionalDatabaseProperties.regions[regionCode]
                    val jdbcUrl = regionConfig?.url?.let { baseUrl ->
                        baseUrl.substringBeforeLast("/") + "/$databaseName"
                    } ?: "jdbc:postgresql://$dbHost:$dbPort/$databaseName"

                    val username = regionConfig?.username ?: dbUsername
                    val password = regionConfig?.password ?: dbPassword

                    val primaryConfig = HikariConfig().apply {
                        this.jdbcUrl = jdbcUrl
                        this.username = username
                        this.password = password
                        schema = kb.schemaName

                        maximumPoolSize = 10
                        minimumIdle = 2
                        connectionTimeout = 30000
                        idleTimeout = 600000
                        maxLifetime = 1800000

                        poolName = "kb-${kb.id}-primary"

                        addDataSourceProperty("cachePrepStmts", "true")
                        addDataSourceProperty("prepStmtCacheSize", "250")
                        addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
                        addDataSourceProperty("stringtype", "unspecified")

                        connectionTestQuery = "SELECT 1"
                        validationTimeout = 5000
                    }

                    val replicaJdbcUrl = regionConfig?.readReplicaUrl?.let { replicaBaseUrl ->
                        replicaBaseUrl.substringBeforeLast("/") + "/$databaseName"
                    } ?: jdbcUrl

                    val replicaConfig = HikariConfig().apply {
                        this.jdbcUrl = replicaJdbcUrl
                        this.username = username
                        this.password = password
                        schema = kb.schemaName

                        maximumPoolSize = 8
                        minimumIdle = 1
                        connectionTimeout = 30000
                        idleTimeout = 600000
                        maxLifetime = 1800000

                        poolName = "kb-${kb.id}-replica"

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

                    log.info { "Knowledge base $knowledgeBaseId (region: $regionCode) routing configured: writes -> kb-${kb.id}-primary, reads -> kb-${kb.id}-replica" }
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
                                log.info { "Closing HikariDataSource: ${ds.poolName}" }
                                ds.close()
                            }
                        }
                    }
                    is HikariDataSource -> {
                        log.info { "Closing HikariDataSource: ${dataSource.poolName}" }
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

        log.info { "Master database routing configured: writes -> master-primary-pool, reads -> master-replica-pool" }
        return routingDataSource
    }

    @Bean
    @Primary
    fun knowledgeBaseEntityManagerFactory(
        builder: EntityManagerFactoryBuilder,
        @Qualifier("knowledgeBaseDataSourceRouter") knowledgeBaseDataSourceRouter: DataSource,
        jpaProperties: JpaProperties
    ): LocalContainerEntityManagerFactoryBean {
        return builder
            .dataSource(knowledgeBaseDataSourceRouter)
            .packages(
                "ai.sovereignrag.chat.domain",
                "ai.sovereignrag.domain",
                "ai.sovereignrag.client.domain"
            )
            .persistenceUnit("knowledgeBase")
            .properties(jpaProperties.properties)
            .build()
    }

    @Bean(value = ["knowledgeBaseTransactionManager", "transactionManager"])
    @Primary
    fun knowledgeBaseTransactionManager(
        @Qualifier("knowledgeBaseEntityManagerFactory") knowledgeBaseEntityManagerFactory: EntityManagerFactory
    ): PlatformTransactionManager {
        return JpaTransactionManager(knowledgeBaseEntityManagerFactory)
    }
}
