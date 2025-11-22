package nl.compilot.ai.commons.agent.memory

/**
 * Chat memory interface for managing conversation history.
 *
 * Implementations must handle:
 * - Adding messages (user and assistant)
 * - Retrieving conversation history
 * - Optional persistence (in-memory, Redis, database, etc.)
 * - Optional message limits (window size)
 *
 * This interface is framework-agnostic and can be implemented with
 * various storage backends.
 */
interface ChatMemory {

    /**
     * Add a user message to the conversation history.
     *
     * @param message The user's message text
     * @param metadata Optional metadata (e.g., timestamp, user ID)
     */
    fun addUserMessage(message: String, metadata: Map<String, Any> = emptyMap())

    /**
     * Add an assistant message to the conversation history.
     *
     * @param message The assistant's message text
     * @param metadata Optional metadata (e.g., timestamp, confidence, sources)
     */
    fun addAssistantMessage(message: String, metadata: Map<String, Any> = emptyMap())

    /**
     * Add a system message to the conversation history.
     *
     * System messages are typically used for context injection or
     * conversation control.
     *
     * @param message The system message text
     * @param metadata Optional metadata
     */
    fun addSystemMessage(message: String, metadata: Map<String, Any> = emptyMap())

    /**
     * Get all messages in the conversation history.
     *
     * @return List of messages in chronological order
     */
    fun getAllMessages(): List<ChatMessage>

    /**
     * Get the last N messages from the conversation history.
     *
     * @param limit Maximum number of messages to return
     * @return List of recent messages in chronological order
     */
    fun getRecentMessages(limit: Int): List<ChatMessage>

    /**
     * Clear all messages from the conversation history.
     */
    fun clear()

    /**
     * Get the number of messages in the conversation history.
     */
    fun size(): Int

    /**
     * Check if the conversation history is empty.
     */
    fun isEmpty(): Boolean = size() == 0

    /**
     * Check if the conversation history is not empty.
     */
    fun isNotEmpty(): Boolean = size() > 0
}
