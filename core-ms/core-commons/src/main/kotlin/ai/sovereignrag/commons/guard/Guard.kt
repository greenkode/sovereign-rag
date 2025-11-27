package ai.sovereignrag.commons.guard

/**
 * Base interface for all guards
 * Guards validate and control tool executions
 */
interface Guard {
    /**
     * Unique name for this guard
     */
    val name: String

    /**
     * Guard type/category (e.g., "RATE_LIMIT", "AUTHORIZATION", "VALIDATION")
     */
    val type: String

    /**
     * Priority/order of execution (lower numbers execute first)
     * Use this to control guard execution order in a chain
     */
    val priority: Int get() = 100

    /**
     * Execute the guard check
     * @param context Current request context
     * @return GuardResult indicating whether to allow, deny, or require confirmation
     */
    suspend fun execute(context: GuardContext): GuardResult

    /**
     * Check if this guard applies to the given context
     * Allows guards to opt-out of execution based on context
     * @param context Current request context
     * @return true if this guard should execute, false to skip
     */
    fun appliesTo(context: GuardContext): Boolean = true

    /**
     * Optional description of what this guard does
     */
    val description: String get() = "No description provided"
}

/**
 * Abstract base class for guards with common functionality
 */
abstract class AbstractGuard(
    override val name: String,
    override val type: String,
    override val priority: Int = 100
) : Guard {

    /**
     * Create a Deny result
     */
    protected fun deny(
        reason: String,
        code: String,
        severity: GuardResult.Severity = GuardResult.Severity.ERROR,
        metadata: Map<String, Any> = emptyMap()
    ): GuardResult.Deny = GuardResult.Deny(reason, code, severity, metadata)

    /**
     * Create an Allow result
     */
    protected fun allow(): GuardResult = GuardResult.Allow

    /**
     * Create a RequireConfirmation result
     */
    protected fun requireConfirmation(
        message: String,
        context: Map<String, Any> = emptyMap()
    ): GuardResult.RequireConfirmation = GuardResult.RequireConfirmation(message, context)
}
