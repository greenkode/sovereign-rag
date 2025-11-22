package nl.compilot.ai.commons.agent

import nl.compilot.ai.commons.agent.memory.ChatMemory
import nl.compilot.ai.commons.agent.persona.Persona
import java.time.Instant

/**
 * Context passed to the agent for each message processing request.
 *
 * Contains all information needed to process a user message:
 * - The user's message
 * - Session/conversation metadata
 * - Chat memory (conversation history)
 * - Persona (behavioral configuration)
 * - Optional tenant/user identification
 *
 * This is designed to be immutable and lightweight.
 */
data class AgentContext(
    /**
     * The user's message text
     */
    val message: String,

    /**
     * Unique session/conversation ID
     */
    val sessionId: String,

    /**
     * Chat memory containing conversation history.
     * The agent will read from this and add new messages to it.
     */
    val memory: ChatMemory,

    /**
     * Persona defining the agent's behavior, personality, and capabilities
     */
    val persona: Persona,

    /**
     * Optional user ID for personalization
     */
    val userId: String? = null,

    /**
     * Optional tenant ID for multi-tenancy
     */
    val tenantId: String? = null,

    /**
     * Optional language preference (e.g., "en", "nl", "de")
     * If null or "auto", the agent should auto-detect from the message
     */
    val language: String? = null,

    /**
     * Timestamp of the message
     */
    val timestamp: Instant = Instant.now(),

    /**
     * Additional metadata (extensibility)
     */
    val metadata: Map<String, Any> = emptyMap()
)
