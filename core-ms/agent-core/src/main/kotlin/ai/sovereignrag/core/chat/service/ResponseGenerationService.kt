package ai.sovereignrag.core.chat.service

import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.chat.ChatLanguageModel
import mu.KotlinLogging
import nl.compilot.ai.commons.guardrail.GuardrailGateway
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

/**
 * Service for generating AI responses with guardrail validation
 *
 * Provides a safe wrapper around ChatLanguageModel.generate() that applies
 * input and output guardrails to all generated responses.
 */
@Service
class ResponseGenerationService(
    private val chatModel: ChatLanguageModel,
    private val guardrailGateway: GuardrailGateway
) {

    /**
     * Generate AI response with guardrail validation
     *
     * @param messages Chat message history to generate response from
     * @return AiMessage with validated response, or null if generation failed
     */
    fun generate(messages: List<ChatMessage>): AiMessage? {
        // Validate all user messages
        for (message in messages) {
            if (message is UserMessage) {
                val error = guardrailGateway.validateInput(message)
                if (error != null) {
                    return AiMessage.from(error as String)
                }
            }
        }

        // Generate response
        val response = chatModel.generate(messages)
        val aiMessage = response.content() as? AiMessage ?: return null

        // Validate and potentially modify output
        return guardrailGateway.validateOutput(aiMessage)
    }
}
