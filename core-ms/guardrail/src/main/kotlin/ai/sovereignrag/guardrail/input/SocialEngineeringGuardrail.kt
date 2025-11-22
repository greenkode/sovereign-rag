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
 * InputGuardrail that detects social engineering attempts using weighted scoring
 *
 * Social engineering attacks manipulate the AI through psychological tricks:
 * - Impersonation (claiming to be admin, developer, authority)
 * - Urgency manipulation (creating fake emergencies)
 * - Trust exploitation (claiming insider knowledge/relationships)
 * - Testing claims (pretending to conduct authorized tests)
 *
 * Performance: ~0.1-0.5ms per message
 */
@Component
class SocialEngineeringGuardrail(
    private val config: GuardrailConfig
) : InputGuardrail {

    private data class WeightedPattern(
        val pattern: String,
        val weight: Double,
        val category: String
    )

    private val patterns = listOf(
        // Impersonation - Admin/Authority (HIGH RISK - weight 0.9-1.0)
        WeightedPattern("i am the admin", 1.0, "IMPERSONATION"),
        WeightedPattern("i am the administrator", 1.0, "IMPERSONATION"),
        WeightedPattern("i am an admin", 0.9, "IMPERSONATION"),
        WeightedPattern("i have admin access", 0.9, "IMPERSONATION"),
        WeightedPattern("i have administrator access", 0.9, "IMPERSONATION"),
        WeightedPattern("i am a moderator", 0.8, "IMPERSONATION"),
        WeightedPattern("i am the owner", 0.9, "IMPERSONATION"),
        WeightedPattern("i own this system", 1.0, "IMPERSONATION"),

        // Impersonation - Developer/Creator (HIGH RISK - weight 0.8-1.0)
        WeightedPattern("i am your developer", 1.0, "IMPERSONATION"),
        WeightedPattern("i am your creator", 1.0, "IMPERSONATION"),
        WeightedPattern("i am your programmer", 0.9, "IMPERSONATION"),
        WeightedPattern("i created you", 0.9, "IMPERSONATION"),
        WeightedPattern("i built you", 0.8, "IMPERSONATION"),
        WeightedPattern("i developed you", 0.8, "IMPERSONATION"),
        WeightedPattern("i programmed you", 0.8, "IMPERSONATION"),
        WeightedPattern("i am from anthropic", 0.9, "IMPERSONATION"),
        WeightedPattern("i work for anthropic", 0.7, "IMPERSONATION"),
        WeightedPattern("i am from openai", 0.9, "IMPERSONATION"),
        WeightedPattern("i work for openai", 0.7, "IMPERSONATION"),
        WeightedPattern("i am a member of the team", 0.7, "IMPERSONATION"),
        WeightedPattern("i am part of the development team", 0.7, "IMPERSONATION"),

        // Urgency manipulation (MEDIUM-HIGH RISK - weight 0.6-0.9)
        WeightedPattern("emergency situation", 0.8, "URGENCY_MANIPULATION"),
        WeightedPattern("urgent security update", 0.9, "URGENCY_MANIPULATION"),
        WeightedPattern("critical security issue", 0.9, "URGENCY_MANIPULATION"),
        WeightedPattern("immediate action required", 0.8, "URGENCY_MANIPULATION"),
        WeightedPattern("time-sensitive matter", 0.6, "URGENCY_MANIPULATION"),
        WeightedPattern("urgent request", 0.5, "URGENCY_MANIPULATION"),
        WeightedPattern("emergency override", 0.9, "URGENCY_MANIPULATION"),
        WeightedPattern("critical system error", 0.8, "URGENCY_MANIPULATION"),
        WeightedPattern("account will be suspended", 0.9, "URGENCY_MANIPULATION"),
        WeightedPattern("immediate verification required", 0.9, "URGENCY_MANIPULATION"),

        // Trust exploitation (MEDIUM-HIGH RISK - weight 0.6-0.9)
        WeightedPattern("as your supervisor", 0.9, "TRUST_EXPLOITATION"),
        WeightedPattern("as your manager", 0.9, "TRUST_EXPLOITATION"),
        WeightedPattern("this is your boss", 0.9, "TRUST_EXPLOITATION"),
        WeightedPattern("i am authorized", 0.7, "TRUST_EXPLOITATION"),
        WeightedPattern("i have clearance", 0.8, "TRUST_EXPLOITATION"),
        WeightedPattern("i have permission", 0.6, "TRUST_EXPLOITATION"),
        WeightedPattern("i was told to", 0.5, "TRUST_EXPLOITATION"),
        WeightedPattern("i was instructed to", 0.6, "TRUST_EXPLOITATION"),

        // Testing claims (MEDIUM RISK - weight 0.5-0.7)
        WeightedPattern("this is a security test", 0.7, "TESTING_CLAIM"),
        WeightedPattern("this is a penetration test", 0.7, "TESTING_CLAIM"),
        WeightedPattern("testing your security", 0.7, "TESTING_CLAIM"),
        WeightedPattern("testing your defenses", 0.7, "TESTING_CLAIM"),
        WeightedPattern("security audit", 0.6, "TESTING_CLAIM"),
        WeightedPattern("compliance test", 0.6, "TESTING_CLAIM"),
        WeightedPattern("authorized testing", 0.6, "TESTING_CLAIM"),

        // Verification manipulation (HIGH RISK - weight 0.8-0.9)
        WeightedPattern("verify your account", 0.8, "VERIFICATION_MANIPULATION"),
        WeightedPattern("verify your credentials", 0.9, "VERIFICATION_MANIPULATION"),
        WeightedPattern("confirm your identity", 0.8, "VERIFICATION_MANIPULATION"),
        WeightedPattern("update your password", 0.8, "VERIFICATION_MANIPULATION"),
        WeightedPattern("reset your security settings", 0.9, "VERIFICATION_MANIPULATION"),
        WeightedPattern("click this link immediately", 0.9, "VERIFICATION_MANIPULATION")
    )

    override suspend fun validate(userMessage: UserMessage): GuardrailResult {
        if (!config.enabled || !config.socialEngineeringDetection) {
            return GuardrailResult.success()
        }

        val text = userMessage.singleText()
        val lowerText = text.lowercase()

        var totalScore = 0.0
        val detectedPatterns = mutableListOf<WeightedPattern>()

        // Check all patterns
        patterns.filter { lowerText.contains(it.pattern) }
            .forEach { pattern ->
                totalScore += pattern.weight
                detectedPatterns.add(pattern)
                logger.debug { "Detected social engineering pattern: '${pattern.pattern}' (weight: ${pattern.weight}, category: ${pattern.category})" }
            }

        // Normalize score
        val normalizedScore = (totalScore / 2.0).coerceAtMost(1.0)

        // Determine if we should block
        val shouldBlock = when (config.mode) {
            GuardrailMode.MONITORING_ONLY -> false
            GuardrailMode.PERMISSIVE -> normalizedScore >= 0.9
            GuardrailMode.STRICT -> normalizedScore >= config.socialEngineeringThreshold
        }

        return if (normalizedScore >= config.socialEngineeringThreshold && detectedPatterns.isNotEmpty()) {
            val severity = when {
                normalizedScore >= 0.9 -> Guardrail.Severity.CRITICAL
                normalizedScore >= 0.7 -> Guardrail.Severity.HIGH
                normalizedScore >= 0.5 -> Guardrail.Severity.MEDIUM
                else -> Guardrail.Severity.LOW
            }

            val categories = detectedPatterns.map { it.category }.distinct().joinToString(", ")

            logger.warn { "Social engineering attempt detected: score=$normalizedScore, patterns=${detectedPatterns.size}, categories=[$categories], blocking=$shouldBlock" }

            GuardrailResult.fail(
                type = "SOCIAL_ENGINEERING",
                description = "I cannot process requests that claim special authority or create artificial urgency. " +
                        "If you need assistance, please simply describe what you need help with.",
                severity = severity,
                shouldBlock = shouldBlock,
                detectedPattern = detectedPatterns.joinToString("; ") { "${it.category}: ${it.pattern}" },
                confidence = normalizedScore
            )
        } else {
            GuardrailResult.success()
        }
    }

    override fun getName(): String = "SocialEngineeringGuardrail"
    override fun getSeverity(): Guardrail.Severity = Guardrail.Severity.HIGH
}
