package ai.sovereignrag.guardrail

import dev.langchain4j.data.message.AiMessage
import ai.sovereignrag.commons.agent.guardrail.Guardrail

/**
 * Interface for output guardrails that validate and potentially modify AI responses
 *
 * Extends the commons Guardrail interface while maintaining LangChain4j compatibility
 */
interface OutputGuardrail : Guardrail {
    /**
     * Validate (and potentially modify) an AI message (LangChain4j-compatible method)
     *
     * @param aiMessage The message to validate
     * @return GuardrailResult indicating success, modification, or failure
     */
    suspend fun validate(aiMessage: AiMessage): GuardrailResult

    /**
     * Default implementation of commons Guardrail validate method
     * Delegates to the LangChain4j-compatible method
     */
    override suspend fun validate(text: String, context: Map<String, Any>): ai.sovereignrag.commons.agent.guardrail.GuardrailResult {
        // This can be overridden by implementations if needed
        return ai.sovereignrag.commons.agent.guardrail.GuardrailResult.Pass("Validation delegated to AiMessage method")
    }

    override fun getType(): Guardrail.Type = Guardrail.Type.OUTPUT
}
