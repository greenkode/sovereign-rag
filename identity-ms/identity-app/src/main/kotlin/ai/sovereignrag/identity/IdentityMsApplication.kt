package ai.sovereignrag.identity

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

@EnableAsync
@SpringBootApplication
class IdentityMsApplication

fun main(args: Array<String>) {
	runApplication<IdentityMsApplication>(*args)
}
