package nl.compilot.ai.commons.guard

import java.time.Instant

/**
 * Context information for guard execution
 * Contains all relevant information about the current request and environment
 */
data class GuardContext(
    // Identity
    val tenantId: String,
    val sessionId: String,
    val userId: String? = null,

    // Tool information
    val toolName: String,
    val toolParameters: Map<String, Any> = emptyMap(),

    // Conversation context
    val conversationHistory: List<Message> = emptyList(),
    val currentMessage: String = "",
    val messageCount: Int = 0,

    // User sentiment/state
    val userSentiment: Sentiment = Sentiment.NEUTRAL,
    val isUserFrustrated: Boolean = false,

    // Execution history
    val toolExecutionHistory: List<ToolExecution> = emptyList(),
    val recentFailures: Int = 0,

    // Permissions
    val permissions: Set<Permission> = emptySet(),
    val userRole: UserRole = UserRole.ANONYMOUS,

    // Timing
    val timestamp: Instant = Instant.now(),

    // Additional metadata
    val metadata: Map<String, Any> = emptyMap()
) {
    /**
     * User sentiment classification
     */
    enum class Sentiment {
        FRUSTRATED,
        CONFUSED,
        NEUTRAL,
        SATISFIED
    }

    /**
     * User role for authorization
     */
    enum class UserRole {
        ANONYMOUS,
        AUTHENTICATED,
        ADMIN,
        SUPER_ADMIN
    }

    /**
     * Message in conversation history
     */
    data class Message(
        val role: MessageRole,
        val content: String,
        val timestamp: Instant
    )

    enum class MessageRole {
        USER,
        ASSISTANT,
        SYSTEM
    }

    /**
     * Previous tool execution
     */
    data class ToolExecution(
        val toolName: String,
        val success: Boolean,
        val timestamp: Instant,
        val parameters: Map<String, Any> = emptyMap()
    )

    /**
     * Get tool executions for a specific tool
     */
    fun getExecutionsForTool(toolName: String): List<ToolExecution> =
        toolExecutionHistory.filter { it.toolName == toolName }

    /**
     * Count how many times a tool has been executed in this session
     */
    fun countToolExecutions(toolName: String): Int =
        getExecutionsForTool(toolName).size

    /**
     * Count recent failures for a specific tool
     */
    fun countRecentFailures(toolName: String, withinLast: Int = 3): Int =
        getExecutionsForTool(toolName)
            .takeLast(withinLast)
            .count { !it.success }

    /**
     * Check if a tool has been executed with the same parameters recently
     */
    fun hasRecentDuplicateExecution(toolName: String, parameters: Map<String, Any>): Boolean =
        getExecutionsForTool(toolName)
            .takeLast(3)
            .any { it.parameters == parameters }

    /**
     * Check if user has a specific permission
     */
    fun hasPermission(permission: Permission): Boolean =
        permissions.contains(permission)
}
