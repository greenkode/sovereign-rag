package ai.sovereignrag.guardrail

import dev.langchain4j.data.message.AiMessage
import ai.sovereignrag.commons.agent.guardrail.Guardrail

/**
 * Result of a guardrail validation
 *
 * Aligns with commons Guardrail interface structure while maintaining backward compatibility
 */
sealed class GuardrailResult {

    /**
     * Validation passed without any changes
     */
    data class Pass(
        val message: String = "Validation passed"
    ) : GuardrailResult()

    /**
     * Validation passed but the message was modified (e.g., PII redaction)
     */
    data class PassWith(
        val modifiedMessage: AiMessage,
        val message: String = "Validation passed with modifications"
    ) : GuardrailResult()

    /**
     * Validation failed - violations found
     */
    data class Fail(
        val violations: List<Violation>,
        val blockedText: String? = null,
        val shouldBlock: Boolean = true
    ) : GuardrailResult()

    /**
     * Individual violation detected by a guardrail
     */
    data class Violation(
        val type: String,
        val description: String,
        val severity: Guardrail.Severity,
        val detectedPattern: String? = null,
        val confidence: Double = 1.0,
        val metadata: Map<String, Any> = emptyMap()
    )

    // Convenience methods for backward compatibility
    fun isSuccess(): Boolean = this is Pass || this is PassWith
    fun isFatal(): Boolean = this is Fail && shouldBlock
    fun isPassWith(): Boolean = this is PassWith

    fun errorMessage(): String? = when (this) {
        is Fail -> violations.joinToString("; ") { it.description }
        else -> null
    }

    fun outputMessage(): AiMessage? = when (this) {
        is PassWith -> modifiedMessage
        else -> null
    }

    companion object {
        // Factory methods for backward compatibility
        fun success() = Pass()
        fun successWith(message: AiMessage) = PassWith(message)
        fun fatal(message: String) = Fail(
            violations = listOf(
                Violation(
                    type = "FATAL",
                    description = message,
                    severity = Guardrail.Severity.HIGH
                )
            ),
            shouldBlock = true
        )

        // New factory methods aligned with commons structure
        fun pass(message: String = "Validation passed") = Pass(message)

        fun fail(
            type: String,
            description: String,
            severity: Guardrail.Severity,
            shouldBlock: Boolean = true,
            detectedPattern: String? = null,
            confidence: Double = 1.0
        ) = Fail(
            violations = listOf(
                Violation(
                    type = type,
                    description = description,
                    severity = severity,
                    detectedPattern = detectedPattern,
                    confidence = confidence
                )
            ),
            shouldBlock = shouldBlock
        )
    }
}
