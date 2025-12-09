package ai.sovereignrag.knowledgebase.config

import an.awesome.pipelinr.Command
import an.awesome.pipelinr.CommandHandlers
import an.awesome.pipelinr.Pipeline
import an.awesome.pipelinr.Pipelinr
import jakarta.persistence.EntityManagerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.transaction.PlatformTransactionManager

@TestConfiguration
class IntegrationTestConfiguration {

    @Bean
    fun pipeline(commandHandlers: ObjectProvider<Command.Handler<*, *>>): Pipeline {
        val handlers = CommandHandlers { -> commandHandlers.orderedStream() }
        return Pipelinr().with(handlers)
    }

    @Bean(name = ["masterTransactionManager", "transactionManager"])
    fun masterTransactionManager(entityManagerFactory: EntityManagerFactory): PlatformTransactionManager {
        return JpaTransactionManager(entityManagerFactory)
    }
}
