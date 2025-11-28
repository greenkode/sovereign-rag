package ai.sovereignrag.ingestion

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EntityScan
@EnableJpaRepositories
@ConfigurationPropertiesScan
@EnableScheduling
class IngestionApplication

fun main(args: Array<String>) {
    runApplication<IngestionApplication>(*args)
}
