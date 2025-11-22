package nl.compilot.ai.commons.guardrail

import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.UserMessage

/**
 * Gateway interface for input and output validation using guardrails
 *
 * This abstraction allows the agent-core module to validate messages
 * without depending directly on the guardrail implementations
 */
interface GuardrailGateway {

    /**
     * Validate user input before it reaches the LLM
     *
     * @param userMessage The user message to validate
     * @return Error message if validation failed and request should be blocked, null if validation passed
     */
    fun validateInput(userMessage: UserMessage): String?

    /**
     * Validate and potentially modify AI output before returning to the user
     *
     * @param aiMessage The AI message to validate
     * @return The validated message (potentially modified for safety), or a safe error message if blocked
     */
    fun validateOutput(aiMessage: AiMessage): AiMessage
}
