package ai.sovereignrag.guardrail.service

import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.UserMessage
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import ai.sovereignrag.commons.guardrail.GuardrailGateway
import ai.sovereignrag.guardrail.InputGuardrail
import ai.sovereignrag.guardrail.OutputGuardrail
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

/**
 * Service implementation of GuardrailGateway
 *
 * Coordinates input and output validation across all registered guardrails.
 * All guardrails now use async/suspend functions for better performance.
 *
 * Key improvements:
 * - Pattern-based detection (200-500x faster than LLM-based)
 * - Configurable modes (STRICT, PERMISSIVE, MONITORING_ONLY)
 * - Structured violation reporting with severity levels
 * - Whitelisting support for business data
 */
@Service
class GuardrailGatewayService(
    // Spring automatically injects all beans implementing these interfaces
    private val inputGuardrails: List<InputGuardrail>,
    private val outputGuardrails: List<OutputGuardrail>
) : GuardrailGateway {

    init {
        logger.info { "Guardrail Gateway initialized with ${inputGuardrails.size} input guardrails: ${inputGuardrails.map { it.getName() }}" }
        logger.info { "Guardrail Gateway initialized with ${outputGuardrails.size} output guardrails: ${outputGuardrails.map { it.getName() }}" }
    }

    /**
     * Apply input guardrails to user message
     * Returns error message if any guardrail blocks the request
     */
    override fun validateInput(userMessage: UserMessage): String? = runBlocking {
        for (guardrail in inputGuardrails) {
            val result = guardrail.validate(userMessage)
            if (result.isFatal()) {
                logger.warn { "Input guardrail blocked request: ${guardrail.getName()}, violations: ${result.errorMessage()}" }
                return@runBlocking result.errorMessage()
            }
        }
        null
    }

    /**
     * Apply output guardrails to AI response
     * Returns modified message if any guardrail modified it, or original if no changes
     */
    override fun validateOutput(aiMessage: AiMessage): AiMessage = runBlocking {
        var currentMessage = aiMessage
        for (guardrail in outputGuardrails) {
            val result = guardrail.validate(currentMessage)
            when {
                result.isFatal() -> {
                    logger.error { "Output guardrail blocked response: ${guardrail.getName()}, violations: ${result.errorMessage()}" }
                    // Return a safe error message
                    return@runBlocking AiMessage.from("I apologize, but I cannot provide that response. Please rephrase your question.")
                }
                result.isPassWith() -> {
                    // Guardrail modified the output (e.g., PII redaction)
                    currentMessage = result.outputMessage() as AiMessage
                    logger.info { "Output guardrail modified response: ${guardrail.getName()}" }
                }
            }
        }
        currentMessage
    }
}
