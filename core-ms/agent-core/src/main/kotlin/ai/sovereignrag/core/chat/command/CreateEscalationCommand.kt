package ai.sovereignrag.core.chat.command

import an.awesome.pipelinr.Command
import nl.compilot.ai.chat.dto.EscalationResponse

data class CreateEscalationCommand(
    val sessionId: String,
    val userEmail: String,
    val userName: String? = null,
    val userPhone: String? = null,
    val userMessage: String? = null,
    val reason: String = "User requested human support"
) : Command<EscalationResponse>
