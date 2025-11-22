package ai.sovereignrag.app.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import jakarta.persistence.EntityManagerFactory
import javax.sql.DataSource

/**
 * EntityManager configuration for MASTER database entities
 *
 * This EntityManager is ALWAYS connected to the master database and handles:
 * - Tenant metadata (nl.compilot.ai.tenant.domain)
 * - Prompt templates and personas (nl.compilot.ai.prompt.domain)
 *
 * These entities are shared across all tenants and stored in the master database.
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    basePackages = [
        "nl.compilot.ai.tenant.repository",
        "nl.compilot.ai.prompt.repository"
    ],
    entityManagerFactoryRef = "masterEntityManagerFactory",
    transactionManagerRef = "masterTransactionManager"
)
class MasterEntityManagerConfiguration {

    /**
     * EntityManagerFactory for master database
     * Scans: nl.compilot.ai.tenant.domain, nl.compilot.ai.prompt.domain
     */
    @Bean
    fun masterEntityManagerFactory(
        builder: EntityManagerFactoryBuilder,
        @Qualifier("masterDataSource") masterDataSource: DataSource,
        jpaProperties: JpaProperties
    ): LocalContainerEntityManagerFactoryBean {
        return builder
            .dataSource(masterDataSource)
            .packages(
                "nl.compilot.ai.tenant.domain",
                "nl.compilot.ai.prompt.domain"
            )
            .persistenceUnit("master")
            .properties(jpaProperties.properties)
            .build()
    }

    /**
     * Transaction manager for master database operations
     */
    @Bean
    fun masterTransactionManager(
        @Qualifier("masterEntityManagerFactory") masterEntityManagerFactory: EntityManagerFactory
    ): PlatformTransactionManager {
        return JpaTransactionManager(masterEntityManagerFactory)
    }
}
