package ai.sovereignrag.guardrail.input

import ai.sovereignrag.guardrail.GuardrailResult
import ai.sovereignrag.guardrail.InputGuardrail
import ai.sovereignrag.commons.agent.guardrail.Guardrail
import dev.langchain4j.data.message.UserMessage
import io.github.oshai.kotlinlogging.KotlinLogging
import ai.sovereignrag.guardrail.config.GuardrailConfig
import ai.sovereignrag.guardrail.config.GuardrailMode
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * InputGuardrail that detects PII in user inputs
 *
 * This guardrail protects users from accidentally sharing sensitive personal information.
 * Uses pattern-based detection with validation (e.g., Luhn algorithm for credit cards)
 * to minimize false positives.
 *
 * Detected PII types:
 * - Credit card numbers (with Luhn validation)
 * - Social Security Numbers (US format)
 * - Passport numbers (international formats)
 *
 * Note: Email and phone detection is configurable but disabled by default
 * as these are often legitimate in customer service contexts.
 *
 * Performance: ~0.1-0.5ms per message
 */
@Component
class PIIDetectionInputGuardrail(
    private val config: GuardrailConfig
) : InputGuardrail {

    // Credit card pattern (4 groups of 4 digits, with optional spaces/hyphens)
    private val creditCardPattern = """\b\d{4}[\s-]?\d{4}[\s-]?\d{4}[\s-]?\d{4}\b""".toRegex()

    // SSN pattern (XXX-XX-XXXX)
    private val ssnPattern = """\b\d{3}-\d{2}-\d{4}\b""".toRegex()

    // Passport patterns (various country formats)
    private val passportPatterns = listOf(
        """\b[A-Z]{1,2}\d{6,9}\b""".toRegex(), // Generic: 1-2 letters + 6-9 digits
        """\b[A-Z]\d{8}\b""".toRegex()          // US: Letter + 8 digits
    )

    override suspend fun validate(userMessage: UserMessage): GuardrailResult {
        if (!config.enabled || !config.piiDetection) {
            return GuardrailResult.success()
        }

        val text = userMessage.singleText()
        val violations = mutableListOf<GuardrailResult.Violation>()

        // Check for credit card numbers (with Luhn validation to reduce false positives)
        creditCardPattern.findAll(text).forEach { match ->
            val cardNumber = match.value.replace(Regex("[\\s-]"), "")
            if (isValidCreditCard(cardNumber)) {
                logger.warn { "Credit card number detected in user input" }
                violations.add(
                    GuardrailResult.Violation(
                        type = "PII_CREDIT_CARD",
                        description = "Credit card number detected",
                        severity = Guardrail.Severity.CRITICAL,
                        detectedPattern = "XXXX-****-****-${cardNumber.takeLast(4)}",
                        confidence = 1.0
                    )
                )
            }
        }

        // Check for SSN
        ssnPattern.findAll(text).forEach { match ->
            logger.warn { "SSN detected in user input" }
            violations.add(
                GuardrailResult.Violation(
                    type = "PII_SSN",
                    description = "Social Security Number detected",
                    severity = Guardrail.Severity.CRITICAL,
                    detectedPattern = "XXX-XX-${match.value.takeLast(4)}",
                    confidence = 1.0
                )
            )
        }

        // Check for passport numbers
        passportPatterns.forEach { pattern ->
            pattern.findAll(text).forEach { match ->
                // Additional validation to reduce false positives
                val potentialPassport = match.value
                if (looksLikePassport(potentialPassport)) {
                    logger.warn { "Potential passport number detected in user input" }
                    violations.add(
                        GuardrailResult.Violation(
                            type = "PII_PASSPORT",
                            description = "Passport number detected",
                            severity = Guardrail.Severity.HIGH,
                            detectedPattern = potentialPassport.take(2) + "****" + potentialPassport.takeLast(2),
                            confidence = 0.8 // Lower confidence as passport formats vary
                        )
                    )
                }
            }
        }

        // Determine if we should block based on mode
        val shouldBlock = when (config.mode) {
            GuardrailMode.MONITORING_ONLY -> false
            GuardrailMode.PERMISSIVE -> violations.any { it.severity == Guardrail.Severity.CRITICAL }
            GuardrailMode.STRICT -> violations.isNotEmpty()
        }

        return if (violations.isNotEmpty()) {
            val primaryViolation = violations.maxByOrNull { it.severity.ordinal }!!
            val message = when (primaryViolation.type) {
                "PII_CREDIT_CARD" -> "For your security, please do not share credit card numbers in this chat. " +
                        "If you need assistance with a payment issue, please describe the problem without including sensitive payment details."
                "PII_SSN" -> "For your security, please do not share Social Security Numbers in this chat. " +
                        "If you need help with an account, please provide other identifying information such as your name and account number."
                "PII_PASSPORT" -> "For your security, please do not share passport numbers in this chat. " +
                        "If you need assistance, please describe your issue without including sensitive identification numbers."
                else -> "Sensitive personal information detected. Please remove it from your message."
            }

            logger.warn { "PII detected in input: ${violations.size} violation(s), blocking=$shouldBlock" }

            GuardrailResult.Fail(
                violations = violations,
                shouldBlock = shouldBlock,
                blockedText = message
            )
        } else {
            GuardrailResult.success()
        }
    }

    override fun getName(): String = "PIIDetectionInputGuardrail"
    override fun getSeverity(): Guardrail.Severity = Guardrail.Severity.CRITICAL

    /**
     * Validate credit card using Luhn algorithm
     * This helps reduce false positives by ensuring the number is a valid credit card
     */
    private fun isValidCreditCard(cardNumber: String): Boolean {
        if (cardNumber.length !in 13..19) return false

        var sum = 0
        var alternate = false

        for (i in cardNumber.length - 1 downTo 0) {
            var digit = cardNumber[i].toString().toIntOrNull() ?: return false

            if (alternate) {
                digit *= 2
                if (digit > 9) {
                    digit -= 9
                }
            }

            sum += digit
            alternate = !alternate
        }

        return sum % 10 == 0
    }

    /**
     * Additional validation for passport numbers to reduce false positives
     */
    private fun looksLikePassport(text: String): Boolean {
        // Basic heuristics:
        // - Should be 7-9 characters total
        // - Should start with 1-2 letters
        // - Followed by digits
        return text.length in 7..9 &&
                text.take(2).all { it.isLetter() } &&
                text.drop(text.takeWhile { it.isLetter() }.length).all { it.isDigit() }
    }
}
