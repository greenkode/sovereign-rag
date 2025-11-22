package nl.compilot.ai.commons

import java.util.UUID

/**
 * Interface for logging escalations
 * Implemented in client module, used by core-ai module
 */
interface EscalationLogger {
    /**
     * Log an escalation to the database
     */
    fun logEscalation(
        sessionId: UUID,
        reason: String,
        userEmail: String,
        userName: String,
        userPhone: String?,
        userMessage: String?,
        language: String?,
        persona: String,
        emailSent: Boolean
    ): UUID
}
