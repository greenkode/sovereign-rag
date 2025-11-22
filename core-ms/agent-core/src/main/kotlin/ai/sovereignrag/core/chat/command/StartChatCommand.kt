package ai.sovereignrag.core.chat.command

import an.awesome.pipelinr.Command
import ai.sovereignrag.chat.dto.ChatStartResponse

data class StartChatCommand(
    val persona: String = "customer_service",
    val language: String? = null
) : Command<ChatStartResponse>
