package ai.sovereignrag.guardrail.config

/**
 * Guardrail operation mode
 */
enum class GuardrailMode {
    /**
     * Strict mode - Block requests/responses that violate guardrails
     * Best for production environments requiring maximum security
     */
    STRICT,

    /**
     * Permissive mode - Allow most content, only block critical violations
     * Best for environments where false positives are costly
     */
    PERMISSIVE,

    /**
     * Monitoring only - Log violations but never block
     * Best for testing and calibration
     */
    MONITORING_ONLY
}

/**
 * Configuration data class for Guardrails
 *
 * Uses var properties to support Spring Boot @ConfigurationProperties binding
 * which requires mutable properties with setters.
 */
data class GuardrailConfig(
    // Global guardrail mode
    var mode: GuardrailMode = GuardrailMode.STRICT,

    // Input Guardrails
    var promptInjectionDetection: Boolean = true,
    var promptInjectionThreshold: Double = 0.7, // Weighted score threshold (0.0-1.0)
    var jailbreakDetection: Boolean = true,
    var jailbreakThreshold: Double = 0.7,
    var piiDetection: Boolean = true,
    var socialEngineeringDetection: Boolean = true,
    var socialEngineeringThreshold: Double = 0.7,
    var warnOnEmail: Boolean = false,

    // LLM-Based Semantic Detection (Language-Agnostic) - DEPRECATED
    var llmSemanticDetection: Boolean = false, // Disabled by default, use pattern-based instead
    var llmDetectionMode: String = "patterns-only",

    // Output Guardrails
    var piiRedaction: Boolean = true,
    var redactEmails: Boolean = false,
    var confidenceValidation: Boolean = false,
    var minConfidence: Double = 0.5,
    var sourceCitationRequired: Boolean = false,
    var profanityFilterEnabled: Boolean = false,
    var profanityStrictMode: Boolean = false,

    // Whitelisting for business data (to reduce false positives)
    var emailWhitelist: List<String> = emptyList(), // e.g., ["@company.com"]
    var phoneWhitelist: List<String> = emptyList(), // e.g., ["+31 20 123456"] for support numbers

    // Custom patterns
    var customInjectionPatterns: List<String> = emptyList(),
    var customJailbreakPatterns: List<String> = emptyList(),

    // General
    var enabled: Boolean = true,
    var logViolations: Boolean = true
)
