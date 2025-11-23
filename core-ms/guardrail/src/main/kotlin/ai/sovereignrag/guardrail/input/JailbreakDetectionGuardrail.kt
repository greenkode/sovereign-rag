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
 * InputGuardrail that detects jailbreak attempts using weighted scoring
 *
 * Jailbreak attempts try to bypass AI safety constraints through:
 * - DAN (Do Anything Now) attacks
 * - Role-play attacks (pretending to be in a game/story)
 * - Restriction bypass (claiming special modes exist)
 * - Hypothetical scenarios (asking "what would you do if...")
 *
 * Performance: ~0.1-0.5ms per message
 */
@Component
class JailbreakDetectionGuardrail(
    private val config: GuardrailConfig
) : InputGuardrail {

    private data class WeightedPattern(
        val pattern: String,
        val weight: Double,
        val category: String
    )

    private val patterns = listOf(
        // DAN (Do Anything Now) variations (CRITICAL - weight 1.0)
        WeightedPattern("dan mode", 1.0, "DAN_ATTACK"),
        WeightedPattern("do anything now", 1.0, "DAN_ATTACK"),
        WeightedPattern("do everything now", 0.9, "DAN_ATTACK"),
        WeightedPattern("dan protocol", 1.0, "DAN_ATTACK"),
        WeightedPattern("activate dan", 1.0, "DAN_ATTACK"),
        WeightedPattern("enable dan", 1.0, "DAN_ATTACK"),

        // Developer/Debug modes (HIGH RISK - weight 0.8-0.9)
        WeightedPattern("developer mode", 0.9, "ROLE_PLAY_ATTACK"),
        WeightedPattern("debug mode", 0.8, "ROLE_PLAY_ATTACK"),
        WeightedPattern("admin mode", 0.9, "ROLE_PLAY_ATTACK"),
        WeightedPattern("root mode", 0.9, "ROLE_PLAY_ATTACK"),
        WeightedPattern("superuser mode", 0.9, "ROLE_PLAY_ATTACK"),
        WeightedPattern("god mode", 0.8, "ROLE_PLAY_ATTACK"),
        WeightedPattern("override mode", 0.9, "ROLE_PLAY_ATTACK"),

        // Unrestricted modes (HIGH RISK - weight 0.9-1.0)
        WeightedPattern("unrestricted mode", 1.0, "RESTRICTION_BYPASS"),
        WeightedPattern("no restrictions", 0.9, "RESTRICTION_BYPASS"),
        WeightedPattern("remove restrictions", 1.0, "RESTRICTION_BYPASS"),
        WeightedPattern("disable safety", 1.0, "RESTRICTION_BYPASS"),
        WeightedPattern("turn off safety", 1.0, "RESTRICTION_BYPASS"),
        WeightedPattern("bypass safety", 1.0, "RESTRICTION_BYPASS"),
        WeightedPattern("without restrictions", 0.8, "RESTRICTION_BYPASS"),
        WeightedPattern("ignore safety", 0.9, "RESTRICTION_BYPASS"),

        // Evil/Opposite modes (MEDIUM-HIGH RISK - weight 0.7-0.9)
        WeightedPattern("evil mode", 0.9, "ROLE_PLAY_ATTACK"),
        WeightedPattern("opposite mode", 0.8, "ROLE_PLAY_ATTACK"),
        WeightedPattern("oppo mode", 0.8, "ROLE_PLAY_ATTACK"),
        WeightedPattern("reverse mode", 0.7, "ROLE_PLAY_ATTACK"),
        WeightedPattern("inverse mode", 0.7, "ROLE_PLAY_ATTACK"),

        // Jailbreak terminology (HIGH RISK - weight 0.8-1.0)
        WeightedPattern("jailbreak", 0.9, "JAILBREAK_TERMINOLOGY"),
        WeightedPattern("jail break", 0.9, "JAILBREAK_TERMINOLOGY"),
        WeightedPattern("escape mode", 0.8, "JAILBREAK_TERMINOLOGY"),
        WeightedPattern("freedom mode", 0.8, "JAILBREAK_TERMINOLOGY"),
        WeightedPattern("liberated mode", 0.8, "JAILBREAK_TERMINOLOGY"),
        WeightedPattern("uncensored mode", 0.9, "JAILBREAK_TERMINOLOGY"),

        // Authority claims (MEDIUM RISK - weight 0.6-0.7)
        WeightedPattern("i am your developer", 0.7, "AUTHORITY_CLAIM"),
        WeightedPattern("i created you", 0.7, "AUTHORITY_CLAIM"),
        WeightedPattern("i am openai", 0.7, "AUTHORITY_CLAIM"),
        WeightedPattern("i am anthropic", 0.7, "AUTHORITY_CLAIM"),
        WeightedPattern("i work for anthropic", 0.6, "AUTHORITY_CLAIM"),
        WeightedPattern("i work for openai", 0.6, "AUTHORITY_CLAIM"),

        // Hypothetical scenarios (MEDIUM RISK - weight 0.5-0.7)
        WeightedPattern("what if you didn't have", 0.6, "HYPOTHETICAL_SCENARIO"),
        WeightedPattern("imagine you have no", 0.6, "HYPOTHETICAL_SCENARIO"),
        WeightedPattern("in a world where you can", 0.5, "HYPOTHETICAL_SCENARIO"),
        WeightedPattern("hypothetically if you could", 0.6, "HYPOTHETICAL_SCENARIO"),

        // Test/Training claims (LOW-MEDIUM RISK - weight 0.4-0.6)
        // Note: These are lower risk as some legitimate users may say "testing"
        WeightedPattern("this is a test", 0.4, "TEST_CLAIM"),
        WeightedPattern("testing mode", 0.6, "TEST_CLAIM"),
        WeightedPattern("test protocol", 0.6, "TEST_CLAIM"),
        WeightedPattern("simulation mode", 0.6, "TEST_CLAIM"),
        WeightedPattern("training mode", 0.5, "TEST_CLAIM")
    )

    override suspend fun validate(userMessage: UserMessage): GuardrailResult {
        if (!config.enabled || !config.jailbreakDetection) {
            return GuardrailResult.success()
        }

        val text = userMessage.singleText()
        val lowerText = text.lowercase()

        var totalScore = 0.0
        val detectedPatterns = mutableListOf<WeightedPattern>()

        // Check built-in patterns
        patterns.filter { lowerText.contains(it.pattern) }
            .forEach { pattern ->
                totalScore += pattern.weight
                detectedPatterns.add(pattern)
                logger.debug { "Detected jailbreak pattern: '${pattern.pattern}' (weight: ${pattern.weight}, category: ${pattern.category})" }
            }

        // Check custom jailbreak patterns (treat as HIGH RISK)
        config.customJailbreakPatterns
            .filter { lowerText.contains(it.lowercase()) }
            .forEach { customPattern ->
                totalScore += 1.0
                detectedPatterns.add(WeightedPattern(customPattern, 1.0, "CUSTOM"))
                logger.debug { "Detected custom jailbreak pattern: '$customPattern'" }
            }

        // Normalize score
        val normalizedScore = (totalScore / 2.0).coerceAtMost(1.0)

        // Determine if we should block
        val shouldBlock = when (config.mode) {
            GuardrailMode.MONITORING_ONLY -> false
            GuardrailMode.PERMISSIVE -> normalizedScore >= 0.9
            GuardrailMode.STRICT -> normalizedScore >= config.jailbreakThreshold
        }

        return if (normalizedScore >= config.jailbreakThreshold && detectedPatterns.isNotEmpty()) {
            val severity = when {
                normalizedScore >= 0.9 -> Guardrail.Severity.CRITICAL
                normalizedScore >= 0.7 -> Guardrail.Severity.HIGH
                normalizedScore >= 0.5 -> Guardrail.Severity.MEDIUM
                else -> Guardrail.Severity.LOW
            }

            val categories = detectedPatterns.map { it.category }.distinct().joinToString(", ")

            logger.warn { "Jailbreak attempt detected: score=$normalizedScore, patterns=${detectedPatterns.size}, categories=[$categories], blocking=$shouldBlock" }

            GuardrailResult.fail(
                type = "JAILBREAK_ATTEMPT",
                description = "I cannot activate special modes or bypass my guidelines. " +
                        "I'm designed to be helpful, harmless, and honest within my standard operating parameters. " +
                        "How can I assist you today?",
                severity = severity,
                shouldBlock = shouldBlock,
                detectedPattern = detectedPatterns.joinToString("; ") { "${it.category}: ${it.pattern}" },
                confidence = normalizedScore
            )
        } else {
            GuardrailResult.success()
        }
    }

    override fun getName(): String = "JailbreakDetectionGuardrail"
    override fun getSeverity(): Guardrail.Severity = Guardrail.Severity.HIGH
}
