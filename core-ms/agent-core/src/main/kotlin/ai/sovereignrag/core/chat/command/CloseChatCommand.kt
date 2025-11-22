package ai.sovereignrag.core.chat.command

import an.awesome.pipelinr.Command

data class CloseChatCommand(
    val sessionId: String
) : Command<Map<String, Any>>
