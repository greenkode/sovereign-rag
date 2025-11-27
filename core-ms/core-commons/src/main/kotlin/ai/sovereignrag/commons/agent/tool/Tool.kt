package ai.sovereignrag.commons.agent.tool

/**
 * Tool interface for agent function calling.
 *
 * Tools allow the agent to perform actions and retrieve information
 * beyond simple text generation. Examples:
 * - Send an email
 * - Search a database
 * - Call an external API
 * - Execute a calculation
 *
 * This interface is framework-agnostic and can work with any LLM framework
 * that supports function calling (LangChain4j, OpenAI Functions, etc.)
 */
interface Tool {

    /**
     * Unique name for this tool.
     * Used by the LLM to identify which tool to call.
     */
    fun getName(): String

    /**
     * Human-readable description of what this tool does.
     * The LLM uses this to decide when to use the tool.
     */
    fun getDescription(): String

    /**
     * Execute the tool with the given parameters.
     *
     * @param parameters Map of parameter names to values
     * @return ToolResult containing the execution result
     */
    suspend fun execute(parameters: Map<String, Any>): ToolResult

    /**
     * Get the tool's parameter schema (for LLM function calling).
     * Defines what parameters the tool accepts.
     *
     * @return Map of parameter name to parameter definition
     */
    fun getParameters(): Map<String, ToolParameter>

    /**
     * Check if this tool requires authentication or authorization.
     */
    fun requiresAuth(): Boolean = false
}

/**
 * Tool parameter definition for LLM function calling schema
 */
data class ToolParameter(
    /**
     * Parameter name
     */
    val name: String,

    /**
     * Human-readable description
     */
    val description: String,

    /**
     * Parameter type (string, number, boolean, object, array)
     */
    val type: String,

    /**
     * Whether this parameter is required
     */
    val required: Boolean = true,

    /**
     * Enum values (for parameters with limited options)
     */
    val enumValues: List<String>? = null,

    /**
     * Default value
     */
    val defaultValue: Any? = null
)

/**
 * Result from tool execution
 */
sealed class ToolResult {
    /**
     * Successful tool execution
     */
    data class Success(
        val result: String,
        val metadata: Map<String, Any> = emptyMap()
    ) : ToolResult()

    /**
     * Tool execution failed
     */
    data class Failure(
        val error: String,
        val throwable: Throwable? = null
    ) : ToolResult()
}
