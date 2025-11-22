package ai.sovereignrag.commons.agent.memory

import java.time.Instant

/**
 * Represents a single message in a conversation.
 *
 * Can be a user message, assistant message, or system message.
 */
data class ChatMessage(
    /**
     * The role of the message sender
     */
    val role: Role,

    /**
     * The message text content
     */
    val content: String,

    /**
     * Timestamp of the message
     */
    val timestamp: Instant = Instant.now(),

    /**
     * Optional metadata (e.g., confidence, sources, user ID)
     */
    val metadata: Map<String, Any> = emptyMap()
) {
    /**
     * Message role enum
     */
    enum class Role {
        USER,       // Message from the user
        ASSISTANT,  // Message from the AI assistant
        SYSTEM      // System message (context injection, instructions)
    }
}
