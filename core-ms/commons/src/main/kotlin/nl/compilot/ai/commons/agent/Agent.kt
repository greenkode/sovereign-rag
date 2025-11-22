package nl.compilot.ai.commons.agent

import kotlinx.coroutines.flow.Flow

/**
 * Core Agent interface - framework-agnostic contract for conversational AI agents.
 *
 * Implementations must be stateless - all conversation state is managed externally
 * via the AgentContext (which includes chat memory).
 *
 * This interface supports:
 * - Synchronous (processMessage) and streaming (processMessageStream) modes
 * - Guardrails (input/output validation)
 * - Tool execution
 * - Multi-turn conversations via chat memory
 * - Personas for behavior customization
 */
interface Agent {

    /**
     * Process a single user message and return a complete response.
     *
     * This method:
     * 1. Validates input with guardrails
     * 2. Retrieves relevant context (if RAG is enabled)
     * 3. Invokes the LLM with conversation history
     * 4. Executes any tool calls requested by the LLM
     * 5. Validates output with guardrails
     * 6. Returns the final response
     *
     * @param context The request context including user message, session, and memory
     * @return AgentResponse containing the assistant's reply, confidence, sources, etc.
     * @throws AgentException if processing fails
     */
    suspend fun processMessage(context: AgentContext): AgentResponse

    /**
     * Process a message with streaming response (SSE-style).
     *
     * This method returns a Flow that emits response chunks as they're generated,
     * enabling real-time streaming to the client.
     *
     * @param context The request context including user message, session, and memory
     * @return Flow of AgentResponse chunks (partial responses)
     * @throws AgentException if processing fails
     */
    fun processMessageStream(context: AgentContext): Flow<AgentResponse>

    /**
     * Get the agent's configuration.
     */
    fun getConfig(): AgentConfig
}

/**
 * Exception thrown when agent processing fails.
 */
class AgentException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
