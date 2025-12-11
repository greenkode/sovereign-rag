package ai.sovereignrag.core.rag.streaming

import an.awesome.pipelinr.Command
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

data class StreamingRagChatCommand(
    val conversationId: String?,
    val message: String,
    val knowledgeBaseId: String? = null,
    val sseEmitter: SseEmitter
) : Command<StreamingRagChatResult>
