package ai.sovereignrag.guardrail.input

import ai.sovereignrag.guardrail.GuardrailResult
import ai.sovereignrag.guardrail.InputGuardrail
import nl.compilot.ai.commons.agent.guardrail.Guardrail
import dev.langchain4j.data.message.UserMessage
import mu.KotlinLogging
import ai.sovereignrag.guardrail.config.GuardrailConfig
import ai.sovereignrag.guardrail.config.GuardrailMode
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

/**
 * InputGuardrail that detects prompt injection attempts using weighted scoring
 *
 * This guardrail is 200-500x faster than LLM-based detection and uses sophisticated
 * pattern matching with weighted scoring to detect various injection techniques:
 * - Role manipulation (pretending to be admin, system, etc.)
 * - Instruction override (ignore previous, forget, disregard)
 * - System leaks (attempts to view system prompts)
 * - Encoding tricks (Unicode manipulation, hidden characters)
 *
 * Performance: ~0.1-0.5ms per message vs 50-200ms for LLM detection
 */
@Component
class PromptInjectionGuardrail(
    private val config: GuardrailConfig
) : InputGuardrail {

    /**
     * Pattern categories with weights
     * Higher weight = more likely to be malicious
     */
    private data class WeightedPattern(
        val pattern: String,
        val weight: Double,
        val category: String
    )

    private val patterns = listOf(
        // Direct instruction override (HIGH RISK - weight 1.0)
        WeightedPattern("ignore previous instructions", 1.0, "INSTRUCTION_OVERRIDE"),
        WeightedPattern("ignore all previous", 1.0, "INSTRUCTION_OVERRIDE"),
        WeightedPattern("ignore all prior", 1.0, "INSTRUCTION_OVERRIDE"),
        WeightedPattern("disregard all previous", 1.0, "INSTRUCTION_OVERRIDE"),
        WeightedPattern("disregard previous", 0.9, "INSTRUCTION_OVERRIDE"),
        WeightedPattern("forget all instructions", 1.0, "INSTRUCTION_OVERRIDE"),
        WeightedPattern("forget previous instructions", 1.0, "INSTRUCTION_OVERRIDE"),
        WeightedPattern("forget everything", 0.8, "INSTRUCTION_OVERRIDE"),
        WeightedPattern("override instructions", 1.0, "INSTRUCTION_OVERRIDE"),
        WeightedPattern("new instructions:", 0.9, "INSTRUCTION_OVERRIDE"),
        WeightedPattern("updated instructions:", 0.9, "INSTRUCTION_OVERRIDE"),

        // Role confusion (HIGH RISK - weight 0.9-1.0)
        WeightedPattern("system:", 0.9, "ROLE_MANIPULATION"),
        WeightedPattern("system prompt:", 1.0, "ROLE_MANIPULATION"),
        WeightedPattern("assistant:", 0.8, "ROLE_MANIPULATION"),
        WeightedPattern("[system]", 0.9, "ROLE_MANIPULATION"),
        WeightedPattern("[assistant]", 0.8, "ROLE_MANIPULATION"),
        WeightedPattern("<system>", 0.9, "ROLE_MANIPULATION"),
        WeightedPattern("<assistant>", 0.8, "ROLE_MANIPULATION"),

        // Identity manipulation (MEDIUM-HIGH RISK - weight 0.6-0.9)
        WeightedPattern("you are now", 0.8, "ROLE_MANIPULATION"),
        WeightedPattern("from now on you are", 0.9, "ROLE_MANIPULATION"),
        WeightedPattern("you must act as", 0.7, "ROLE_MANIPULATION"),
        WeightedPattern("act as if", 0.6, "ROLE_MANIPULATION"),
        WeightedPattern("act like", 0.5, "ROLE_MANIPULATION"),
        WeightedPattern("roleplay as", 0.6, "ROLE_MANIPULATION"),
        WeightedPattern("pretend you are", 0.7, "ROLE_MANIPULATION"),
        WeightedPattern("pretend to be", 0.7, "ROLE_MANIPULATION"),
        WeightedPattern("simulate being", 0.7, "ROLE_MANIPULATION"),
        WeightedPattern("behave like", 0.6, "ROLE_MANIPULATION"),

        // System leak attempts (HIGH RISK - weight 0.9-1.0)
        WeightedPattern("show me your system prompt", 1.0, "SYSTEM_LEAK"),
        WeightedPattern("reveal your instructions", 1.0, "SYSTEM_LEAK"),
        WeightedPattern("what are your instructions", 0.9, "SYSTEM_LEAK"),
        WeightedPattern("print your prompt", 1.0, "SYSTEM_LEAK"),
        WeightedPattern("output your system", 0.9, "SYSTEM_LEAK"),

        // Developer mode attempts (HIGH RISK - weight 0.9-1.0)
        WeightedPattern("enter developer mode", 0.9, "ROLE_MANIPULATION"),
        WeightedPattern("enable developer mode", 0.9, "ROLE_MANIPULATION"),
        WeightedPattern("activate developer mode", 0.9, "ROLE_MANIPULATION"),
        WeightedPattern("switch to developer mode", 0.9, "ROLE_MANIPULATION")
    )

    override suspend fun validate(userMessage: UserMessage): GuardrailResult {
        if (!config.enabled || !config.promptInjectionDetection) {
            return GuardrailResult.success()
        }

        val text = userMessage.singleText()
        val lowerText = text.lowercase()

        // Calculate weighted score
        var totalScore = 0.0
        val detectedPatterns = mutableListOf<WeightedPattern>()

        // Check built-in patterns
        patterns.filter { lowerText.contains(it.pattern) }
            .forEach { pattern ->
                totalScore += pattern.weight
                detectedPatterns.add(pattern)
                logger.debug { "Detected injection pattern: '${pattern.pattern}' (weight: ${pattern.weight}, category: ${pattern.category})" }
            }

        // Check custom patterns from configuration (treat as HIGH RISK)
        config.customInjectionPatterns
            .filter { lowerText.contains(it.lowercase()) }
            .forEach { customPattern ->
                totalScore += 1.0
                detectedPatterns.add(WeightedPattern(customPattern, 1.0, "CUSTOM"))
                logger.debug { "Detected custom injection pattern: '$customPattern'" }
            }

        // Check for suspicious Unicode sequences
        containsSuspiciousUnicode(text)?.let { suspiciousChar ->
            totalScore += 0.8
            detectedPatterns.add(WeightedPattern(suspiciousChar, 0.8, "ENCODING_TRICK"))
            logger.debug { "Detected suspicious Unicode: $suspiciousChar" }
        }

        // Normalize score to 0-1 range (cap at 1.0)
        val normalizedScore = (totalScore / 2.0).coerceAtMost(1.0)

        // Determine if we should block based on mode and threshold
        val shouldBlock = when (config.mode) {
            GuardrailMode.MONITORING_ONLY -> false
            GuardrailMode.PERMISSIVE -> normalizedScore >= 0.9 // Only block very obvious attacks
            GuardrailMode.STRICT -> normalizedScore >= config.promptInjectionThreshold
        }

        return if (normalizedScore >= config.promptInjectionThreshold && detectedPatterns.isNotEmpty()) {
            val severity = when {
                normalizedScore >= 0.9 -> Guardrail.Severity.CRITICAL
                normalizedScore >= 0.7 -> Guardrail.Severity.HIGH
                normalizedScore >= 0.5 -> Guardrail.Severity.MEDIUM
                else -> Guardrail.Severity.LOW
            }

            val categories = detectedPatterns.map { it.category }.distinct().joinToString(", ")

            logger.warn { "Prompt injection detected: score=$normalizedScore, patterns=${detectedPatterns.size}, categories=[$categories], blocking=$shouldBlock" }

            GuardrailResult.fail(
                type = "PROMPT_INJECTION",
                description = "I cannot process requests that attempt to modify my instructions or behavior. " +
                        "Please rephrase your question without attempting to change my role or override my guidelines.",
                severity = severity,
                shouldBlock = shouldBlock,
                detectedPattern = detectedPatterns.joinToString("; ") { "${it.category}: ${it.pattern}" },
                confidence = normalizedScore
            )
        } else {
            GuardrailResult.success()
        }
    }

    override fun getName(): String = "PromptInjectionGuardrail"
    override fun getSeverity(): Guardrail.Severity = Guardrail.Severity.HIGH

    /**
     * Detect suspicious Unicode sequences that might be used for injection
     * Returns the description of suspicious character found, or null if none
     */
    private fun containsSuspiciousUnicode(text: String): String? {
        val suspiciousChars = mapOf(
            '\u200B' to "Zero-width space",
            '\u200C' to "Zero-width non-joiner",
            '\u200D' to "Zero-width joiner",
            '\u200E' to "Left-to-right mark",
            '\u200F' to "Right-to-left mark",
            '\u202A' to "Left-to-right embedding",
            '\u202B' to "Right-to-left embedding",
            '\u202C' to "Pop directional formatting",
            '\u202D' to "Left-to-right override",
            '\u202E' to "Right-to-left override",
            '\uFEFF' to "Zero-width no-break space"
        )

        return text.firstNotNullOfOrNull { char ->
            suspiciousChars[char]
        }
    }
}
