package nl.compilot.ai.commons.agent.guardrail

/**
 * Guardrail interface for validating agent inputs and outputs.
 *
 * Guardrails provide security and quality controls:
 * - Input validation (prompt injection, jailbreaks, PII detection)
 * - Output validation (PII redaction, profanity filtering, confidence checks)
 * - Content safety (hate speech, harmful content)
 *
 * Guardrails can be:
 * - Pattern-based (regex, keyword matching) - fast, deterministic
 * - LLM-based (semantic analysis) - slower, more accurate
 * - Hybrid (pattern + LLM) - balanced
 */
interface Guardrail {

    /**
     * Unique name for this guardrail
     */
    fun getName(): String

    /**
     * Validate the input/output text.
     *
     * @param text The text to validate
     * @param context Additional context for validation (optional)
     * @return GuardrailResult indicating if validation passed and any violations
     */
    suspend fun validate(text: String, context: Map<String, Any> = emptyMap()): GuardrailResult

    /**
     * Get the guardrail's severity level
     */
    fun getSeverity(): Severity = Severity.MEDIUM

    /**
     * Get the guardrail's type (input or output)
     */
    fun getType(): Type

    /**
     * Guardrail severity levels
     */
    enum class Severity {
        LOW,     // Warning only
        MEDIUM,  // Block with notification
        HIGH,    // Block and escalate
        CRITICAL // Block, escalate, and log security incident
    }

    /**
     * Guardrail type
     */
    enum class Type {
        INPUT,   // Validates user input before processing
        OUTPUT   // Validates agent output before sending to user
    }
}

/**
 * Result from guardrail validation
 */
sealed class GuardrailResult {
    /**
     * Validation passed - no issues found
     */
    data class Pass(
        val message: String = "Validation passed"
    ) : GuardrailResult()

    /**
     * Validation failed - violations found
     */
    data class Fail(
        val violations: List<Violation>,
        val blockedText: String? = null
    ) : GuardrailResult()

    /**
     * Individual violation
     */
    data class Violation(
        val type: String,
        val description: String,
        val severity: Guardrail.Severity,
        val detectedPattern: String? = null,
        val confidence: Double = 1.0,
        val metadata: Map<String, Any> = emptyMap()
    )
}
