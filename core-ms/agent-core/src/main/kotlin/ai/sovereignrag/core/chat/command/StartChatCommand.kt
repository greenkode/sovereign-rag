package ai.sovereignrag.core.chat.command

import ai.sovereignrag.core.chat.dto.ChatStartResponse
import an.awesome.pipelinr.Command

data class StartChatCommand(
    val persona: String = "customer_service",
    val language: String? = null
) : Command<ChatStartResponse>
