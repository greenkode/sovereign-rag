package ai.sovereignrag.commons.guard

import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Chains multiple guards together and executes them in order
 * Implements the Chain of Responsibility pattern
 */
class GuardChain(
    private val guards: List<Guard>
) {
    /**
     * Execute all guards in the chain
     * Returns the first Deny or RequireConfirmation result, or Allow if all guards pass
     * Guards are executed in priority order (lowest priority number first)
     */
    suspend fun execute(context: GuardContext): GuardChainResult {
        val sortedGuards = guards
            .filter { it.appliesTo(context) }
            .sortedBy { it.priority }

        val results = mutableListOf<GuardExecutionResult>()

        for (guard in sortedGuards) {
            try {
                val startTime = System.currentTimeMillis()
                val result = guard.execute(context)
                val executionTime = System.currentTimeMillis() - startTime

                val executionResult = GuardExecutionResult(
                    guard = guard,
                    result = result,
                    executionTimeMs = executionTime
                )
                results.add(executionResult)

                logger.debug {
                    "[${context.tenantId}][${context.sessionId}] Guard ${guard.name}: ${result.javaClass.simpleName} (${executionTime}ms)"
                }

                // Fail fast: if any guard denies or requires confirmation, stop execution
                when (result) {
                    is GuardResult.Deny -> {
                        logger.warn {
                            "[${context.tenantId}][${context.sessionId}] Guard ${guard.name} denied: ${result.reason}"
                        }
                        return GuardChainResult(
                            overallResult = result,
                            executedGuards = results,
                            totalExecutionTimeMs = results.sumOf { it.executionTimeMs }
                        )
                    }
                    is GuardResult.RequireConfirmation -> {
                        logger.info {
                            "[${context.tenantId}][${context.sessionId}] Guard ${guard.name} requires confirmation: ${result.message}"
                        }
                        return GuardChainResult(
                            overallResult = result,
                            executedGuards = results,
                            totalExecutionTimeMs = results.sumOf { it.executionTimeMs }
                        )
                    }
                    is GuardResult.Allow -> {
                        // Continue to next guard
                    }
                }
            } catch (e: Exception) {
                logger.error(e) {
                    "[${context.tenantId}][${context.sessionId}] Guard ${guard.name} threw exception"
                }

                // Treat exceptions as denials (fail-secure)
                val errorResult = GuardResult.Deny(
                    reason = "Guard execution failed: ${e.message}",
                    code = "GUARD_EXECUTION_ERROR",
                    severity = GuardResult.Severity.CRITICAL,
                    metadata = mapOf(
                        "guardName" to guard.name,
                        "exception" to e.javaClass.simpleName
                    )
                )

                results.add(
                    GuardExecutionResult(
                        guard = guard,
                        result = errorResult,
                        executionTimeMs = 0,
                        error = e
                    )
                )

                return GuardChainResult(
                    overallResult = errorResult,
                    executedGuards = results,
                    totalExecutionTimeMs = results.sumOf { it.executionTimeMs }
                )
            }
        }

        // All guards passed
        return GuardChainResult(
            overallResult = GuardResult.Allow,
            executedGuards = results,
            totalExecutionTimeMs = results.sumOf { it.executionTimeMs }
        )
    }

    companion object {
        /**
         * Builder for creating a guard chain
         */
        fun builder(): Builder = Builder()
    }

    class Builder {
        private val guards = mutableListOf<Guard>()

        fun addGuard(guard: Guard): Builder {
            guards.add(guard)
            return this
        }

        fun addGuards(vararg guards: Guard): Builder {
            this.guards.addAll(guards)
            return this
        }

        fun addGuards(guards: List<Guard>): Builder {
            this.guards.addAll(guards)
            return this
        }

        fun build(): GuardChain = GuardChain(guards.toList())
    }
}

/**
 * Result of executing a guard chain
 */
data class GuardChainResult(
    val overallResult: GuardResult,
    val executedGuards: List<GuardExecutionResult>,
    val totalExecutionTimeMs: Long
) {
    fun isAllowed(): Boolean = overallResult.isAllowed()
    fun isDenied(): Boolean = overallResult.isDenied()
    fun requiresConfirmation(): Boolean = overallResult.requiresConfirmation()

    /**
     * Get the first denial if any
     */
    fun getDenial(): GuardResult.Deny? = overallResult as? GuardResult.Deny

    /**
     * Get all guards that were executed
     */
    fun getExecutedGuardNames(): List<String> = executedGuards.map { it.guard.name }
}

/**
 * Result of executing a single guard
 */
data class GuardExecutionResult(
    val guard: Guard,
    val result: GuardResult,
    val executionTimeMs: Long,
    val error: Exception? = null
)
