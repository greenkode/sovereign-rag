package ai.sovereignrag.guardrail.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Spring configuration for guardrails
 * Provides GuardrailConfig bean with properties binding
 */
@Configuration
class GuardrailConfiguration {

    /**
     * Creates GuardrailConfig bean from application properties
     * Properties are bound from sovereignrag.guardrails.* in application.yml/properties
     */
    @Bean
    @ConfigurationProperties(prefix = "sovereignrag.guardrails")
    fun guardrailConfig(): GuardrailConfig {
        return GuardrailConfig()
    }
}
