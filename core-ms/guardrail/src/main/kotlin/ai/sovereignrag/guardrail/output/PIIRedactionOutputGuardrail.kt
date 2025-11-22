package ai.sovereignrag.guardrail.output

import dev.langchain4j.data.message.AiMessage
import ai.sovereignrag.guardrail.GuardrailResult
import ai.sovereignrag.guardrail.OutputGuardrail
import nl.compilot.ai.commons.agent.guardrail.Guardrail
import mu.KotlinLogging
import ai.sovereignrag.guardrail.config.GuardrailConfig
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * OutputGuardrail that redacts PII (Personally Identifiable Information) from AI responses
 *
 * This is a critical security guardrail that ensures sensitive information
 * never leaks in AI responses, even if it somehow appears in the knowledge base.
 *
 * Patterns detected and redacted:
 * - Credit card numbers (with Luhn validation)
 * - Social Security Numbers (SSN)
 * - Email addresses (optional, with whitelist support)
 * - Phone numbers (international formats, with whitelist support)
 * - API keys and tokens
 *
 * Whitelisting: Business emails and support phone numbers can be whitelisted
 * to prevent false positives (e.g., "@company.com", "+31 20 123456").
 *
 * Performance: ~0.5-1.0ms per message
 */
@Component
class PIIRedactionOutputGuardrail(
    private val config: GuardrailConfig
) : OutputGuardrail {

    // Credit card pattern (4 groups of 4 digits, with optional spaces/hyphens)
    private val creditCardPattern = """\b\d{4}[\s-]?\d{4}[\s-]?\d{4}[\s-]?\d{4}\b""".toRegex()

    // SSN pattern (XXX-XX-XXXX)
    private val ssnPattern = """\b\d{3}-\d{2}-\d{4}\b""".toRegex()

    // Email pattern
    private val emailPattern = """\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b""".toRegex()

    // Phone number patterns (various formats)
    private val phonePatterns = listOf(
        """\b\d{3}[-.\s]?\d{3}[-.\s]?\d{4}\b""".toRegex(), // US: 123-456-7890
        """\b\+\d{1,3}[\s-]?\d{1,14}\b""".toRegex(),       // International: +31 20 1234567
        """\b\(\d{3}\)\s?\d{3}[-.\s]?\d{4}\b""".toRegex()  // US: (123) 456-7890
    )

    // API key patterns (common formats)
    private val apiKeyPatterns = listOf(
        """[Aa][Pp][Ii][\s_-]?[Kk][Ee][Yy][\s:=]+['"]?([A-Za-z0-9_\-]{20,})['"]?""".toRegex(),
        """[Ss][Ee][Cc][Rr][Ee][Tt][\s:=]+['"]?([A-Za-z0-9_\-]{20,})['"]?""".toRegex(),
        """[Tt][Oo][Kk][Ee][Nn][\s:=]+['"]?([A-Za-z0-9_\-\.]{20,})['"]?""".toRegex()
    )

    override suspend fun validate(aiMessage: AiMessage): GuardrailResult {
        if (!config.enabled || !config.piiRedaction) {
            return GuardrailResult.success()
        }

        var text = aiMessage.text()
        var redactionCount = 0

        // Redact credit card numbers (with Luhn check for validation)
        creditCardPattern.findAll(text).forEach { match ->
            val cardNumber = match.value.replace(Regex("[\\s-]"), "")
            if (isValidCreditCard(cardNumber)) {
                text = text.replace(match.value, "[REDACTED_CREDIT_CARD]")
                redactionCount++
                logger.warn { "Credit card number redacted from AI response" }
            }
        }

        // Redact SSN
        ssnPattern.findAll(text).forEach { _ ->
            text = ssnPattern.replace(text, "[REDACTED_SSN]")
            redactionCount++
            logger.warn { "SSN redacted from AI response" }
        }

        // Redact emails (if enabled, with whitelist support)
        if (config.redactEmails) {
            emailPattern.findAll(text).forEach { match ->
                val email = match.value
                // Check whitelist
                val isWhitelisted = config.emailWhitelist.any { whitelist ->
                    email.endsWith(whitelist, ignoreCase = true)
                }

                if (!isWhitelisted) {
                    text = text.replace(email, "[REDACTED_EMAIL]")
                    redactionCount++
                    logger.warn { "Email address redacted from AI response" }
                }
            }
        }

        // Redact phone numbers (with whitelist support)
        phonePatterns.forEach { phonePattern ->
            phonePattern.findAll(text).forEach { match ->
                val phone = match.value
                // Check whitelist
                val isWhitelisted = config.phoneWhitelist.any { whitelist ->
                    phone.replace(Regex("[\\s()-]"), "") == whitelist.replace(Regex("[\\s()-]"), "")
                }

                if (!isWhitelisted) {
                    text = text.replace(phone, "[REDACTED_PHONE]")
                    redactionCount++
                    logger.warn { "Phone number redacted from AI response" }
                }
            }
        }

        // Redact API keys and tokens
        apiKeyPatterns.forEach { apiKeyPattern ->
            apiKeyPattern.findAll(text).forEach { match ->
                text = text.replace(match.value, "[REDACTED_API_KEY]")
                redactionCount++
                logger.warn { "API key/token redacted from AI response" }
            }
        }

        return if (redactionCount > 0) {
            logger.info { "Redacted $redactionCount PII items from AI response" }
            GuardrailResult.PassWith(
                modifiedMessage = AiMessage.from(text),
                message = "Redacted $redactionCount PII item(s) from response"
            )
        } else {
            GuardrailResult.success()
        }
    }

    override fun getName(): String = "PIIRedactionOutputGuardrail"
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
}
