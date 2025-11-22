package ai.sovereignrag.core.chat.command

import an.awesome.pipelinr.Command
import ai.sovereignrag.core.chat.dto.ChatMessageResponse

data class SendMessageCommand(
    val sessionId: String,
    val message: String,
    val useGeneralKnowledge: Boolean = true,
    val showGkDisclaimer: Boolean = false,
    val gkDisclaimerText: String? = null,
    val showSources: Boolean = true
) : Command<ChatMessageResponse>
