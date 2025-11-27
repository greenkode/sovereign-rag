package ai.sovereignrag.notification.app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication(scanBasePackages = ["ai.sovereignrag.notification"])
@EntityScan(basePackages = ["ai.sovereignrag.notification.core.entity"])
@EnableJpaRepositories(basePackages = ["ai.sovereignrag.notification.core.repository"])
class NotificationApplication

fun main(args: Array<String>) {
    runApplication<NotificationApplication>(*args)
}
