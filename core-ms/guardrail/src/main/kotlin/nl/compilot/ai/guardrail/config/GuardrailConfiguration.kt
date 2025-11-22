package nl.compilot.ai.guardrail.config

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
     * Properties are bound from compilot.guardrails.* in application.yml/properties
     */
    @Bean
    @ConfigurationProperties(prefix = "compilot.guardrails")
    fun guardrailConfig(): GuardrailConfig {
        return GuardrailConfig()
    }
}
