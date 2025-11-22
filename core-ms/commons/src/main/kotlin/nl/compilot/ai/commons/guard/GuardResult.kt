package nl.compilot.ai.commons.guard

/**
 * Result of a guard execution
 */
sealed class GuardResult {
    /**
     * Guard allows the operation to proceed
     */
    data object Allow : GuardResult()

    /**
     * Guard denies the operation
     * @param reason Human-readable reason for denial
     * @param code Machine-readable error code
     * @param severity How serious the violation is
     * @param metadata Additional context about the denial
     */
    data class Deny(
        val reason: String,
        val code: String,
        val severity: Severity = Severity.ERROR,
        val metadata: Map<String, Any> = emptyMap()
    ) : GuardResult()

    /**
     * Guard requires user confirmation before proceeding
     * @param message Message to show to user
     * @param context Additional context for confirmation
     */
    data class RequireConfirmation(
        val message: String,
        val context: Map<String, Any> = emptyMap()
    ) : GuardResult()

    /**
     * Severity of a guard violation
     */
    enum class Severity {
        INFO,       // Informational only
        WARNING,    // Should be monitored
        ERROR,      // Clear violation, should be blocked
        CRITICAL    // Security incident, requires immediate attention
    }

    /**
     * Check if this result allows the operation
     */
    fun isAllowed(): Boolean = this is Allow

    /**
     * Check if this result denies the operation
     */
    fun isDenied(): Boolean = this is Deny

    /**
     * Check if this result requires confirmation
     */
    fun requiresConfirmation(): Boolean = this is RequireConfirmation
}
