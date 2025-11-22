package ai.sovereignrag.app.config

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Main Spring Boot Application
 *
 * Component scanning includes:
 * - ai.sovereignrag.* (app, core-ai, etc.)
 * - ai.sovereignrag.guardrail.* (guardrail module)
 * - ai.sovereignrag.security.* (auth module)
 */
@SpringBootApplication(scanBasePackages = ["ai.sovereignrag"])
class SovereignRagApplication

fun main(args: Array<String>) {
    runApplication<SovereignRagApplication>(*args)
}
