package ai.sovereignrag.core.rag.command

import an.awesome.pipelinr.Command

data class RagChatCommand(
    val conversationId: String?,
    val message: String,
    val knowledgeBaseId: String? = null
) : Command<RagChatResult>
