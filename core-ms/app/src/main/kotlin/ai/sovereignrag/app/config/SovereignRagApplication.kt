package ai.sovereignrag.app.config

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Main Spring Boot Application
 *
 * Component scanning includes:
 * - nl.compilot.ai.* (app, core-ai, etc.)
 * - nl.compilot.ai.guardrail.* (guardrail module)
 * - nl.compilot.ai.security.* (auth module)
 */
@SpringBootApplication(scanBasePackages = ["nl.compilot.ai"])
class SovereignRagApplication

fun main(args: Array<String>) {
    runApplication<SovereignRagApplication>(*args)
}
