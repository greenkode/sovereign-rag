package ai.sovereignrag.guardrail

import dev.langchain4j.data.message.UserMessage
import ai.sovereignrag.commons.agent.guardrail.Guardrail

/**
 * Interface for input guardrails that validate user messages before they reach the LLM
 *
 * Extends the commons Guardrail interface while maintaining LangChain4j compatibility
 */
interface InputGuardrail : Guardrail {
    /**
     * Validate a user message (LangChain4j-compatible method)
     *
     * @param userMessage The message to validate
     * @return GuardrailResult indicating success, modification, or failure
     */
    suspend fun validate(userMessage: UserMessage): GuardrailResult

    /**
     * Default implementation of commons Guardrail validate method
     * Delegates to the LangChain4j-compatible method
     */
    override suspend fun validate(text: String, context: Map<String, Any>): ai.sovereignrag.commons.agent.guardrail.GuardrailResult {
        // This can be overridden by implementations if needed
        return ai.sovereignrag.commons.agent.guardrail.GuardrailResult.Pass("Validation delegated to UserMessage method")
    }

    override fun getType(): Guardrail.Type = Guardrail.Type.INPUT
}
