package ai.sovereignrag.notification.app.config.db

import jakarta.persistence.EntityManagerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.domain.AuditorAware
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import java.util.Optional
import javax.sql.DataSource

@Configuration
@EnableTransactionManagement
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
@EnableJpaRepositories(
    basePackages = ["ai.sovereignrag.notification.core.repository"],
    entityManagerFactoryRef = "entityManagerFactory",
    transactionManagerRef = "transactionManager"
)
class JpaConfiguration {

    @Bean
    fun auditorAware(): AuditorAware<String> {
        return AuditorAware { Optional.of("system") }
    }

    @Primary
    @Bean(name = ["entityManagerFactory"])
    fun entityManagerFactory(
        builder: EntityManagerFactoryBuilder,
        @Qualifier("dataSource") dataSource: DataSource
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
            .packages("ai.sovereignrag.notification.core.entity")
            .persistenceUnit("notification")
            .properties(properties)
            .build()
    }

    @Primary
    @Bean(name = ["transactionManager"])
    fun transactionManager(
        @Qualifier("entityManagerFactory") entityManagerFactory: EntityManagerFactory
    ): PlatformTransactionManager {
        return JpaTransactionManager(entityManagerFactory)
    }
}
