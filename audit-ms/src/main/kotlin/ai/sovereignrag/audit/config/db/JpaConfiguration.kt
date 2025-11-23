package ai.sovereignrag.audit.config.db

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.FilterType
import org.springframework.context.annotation.Primary
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import jakarta.persistence.EntityManagerFactory
import javax.sql.DataSource

@Configuration
@EnableTransactionManagement
class JpaConfiguration {

    @Primary
    @Bean(name = ["mainEntityManagerFactory"])
    fun mainEntityManagerFactory(
        builder: EntityManagerFactoryBuilder,
        @Qualifier("mainDataSource") dataSource: DataSource
    ): LocalContainerEntityManagerFactoryBean {
        val properties = hashMapOf<String, Any>(
            "hibernate.dialect" to "org.hibernate.dialect.PostgreSQLDialect",
            "hibernate.hbm2ddl.auto" to "validate",
            "hibernate.show_sql" to false,
            "hibernate.physical_naming_strategy" to "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy",
            "hibernate.implicit_naming_strategy" to "org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy"
        )

        return builder
            .dataSource(dataSource)
            .packages("ai.sovereignrag.audit")
            .persistenceUnit("main")
            .properties(properties)
            .build()
    }

    @Primary
    @Bean(name = ["mainTransactionManager"])
    fun mainTransactionManager(
        @Qualifier("mainEntityManagerFactory") entityManagerFactory: EntityManagerFactory
    ): PlatformTransactionManager {
        return JpaTransactionManager(entityManagerFactory)
    }

    @Bean(name = ["readReplicaEntityManagerFactory"])
    fun readReplicaEntityManagerFactory(
        builder: EntityManagerFactoryBuilder,
        @Qualifier("readReplicaDataSource") dataSource: DataSource
    ): LocalContainerEntityManagerFactoryBean {
        val properties = hashMapOf<String, Any>(
            "hibernate.dialect" to "org.hibernate.dialect.PostgreSQLDialect",
            "hibernate.hbm2ddl.auto" to "validate",
            "hibernate.show_sql" to false,
            "hibernate.physical_naming_strategy" to "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy",
            "hibernate.implicit_naming_strategy" to "org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy"
        )

        return builder
            .dataSource(dataSource)
            .packages("ai.sovereignrag.audit.domain")
            .persistenceUnit("readReplica")
            .properties(properties)
            .build()
    }

    @Bean(name = ["readReplicaTransactionManager"])
    fun readReplicaTransactionManager(
        @Qualifier("readReplicaEntityManagerFactory") entityManagerFactory: EntityManagerFactory
    ): PlatformTransactionManager {
        return JpaTransactionManager(entityManagerFactory)
    }
}

@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(
    basePackages = ["ai.sovereignrag.audit"],
    entityManagerFactoryRef = "mainEntityManagerFactory",
    transactionManagerRef = "mainTransactionManager",
    excludeFilters = [ComponentScan.Filter(type = FilterType.REGEX, pattern = [".*\\.query\\..*"])]
)
class MainRepositoryConfiguration

@Configuration
@EnableJpaRepositories(
    basePackages = ["ai.sovereignrag.audit.domain.query"],
    entityManagerFactoryRef = "readReplicaEntityManagerFactory",
    transactionManagerRef = "readReplicaTransactionManager"
)
class ReadReplicaRepositoryConfiguration