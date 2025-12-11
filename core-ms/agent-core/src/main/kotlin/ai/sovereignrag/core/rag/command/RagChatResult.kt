package ai.sovereignrag.core.rag.command

import java.util.UUID

data class RagChatResult(
    val queryId: UUID,
    val conversationId: String,
    val answer: String,
    val modelUsed: String,
    val modelId: String,
    val processingTimeMs: Long
)
