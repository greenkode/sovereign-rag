package ai.sovereignrag.core.rag.streaming

import java.util.UUID

data class StreamingRagChatResult(
    val queryId: UUID,
    val conversationId: String,
    val modelUsed: String,
    val modelId: String
)
