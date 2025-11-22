package ai.sovereignrag.core.chat.command

import an.awesome.pipelinr.Command
import nl.compilot.ai.chat.dto.ChatStartResponse

data class StartChatCommand(
    val persona: String = "customer_service",
    val language: String? = null
) : Command<ChatStartResponse>
