package ai.sovereignrag.core.rag

import ai.sovereignrag.core.rag.command.RagChatCommand
import ai.sovereignrag.core.rag.query.GetAvailableModelsQuery
import ai.sovereignrag.core.rag.streaming.StreamingRagChatCommand
import ai.sovereignrag.knowledgebase.configuration.dto.LlmModelDto
import an.awesome.pipelinr.Pipeline
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.UUID

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/v1/knowledge-bases/{knowledgeBaseId}")
class RagChatController(
    private val pipeline: Pipeline
) {

    @PostMapping("/chat")
    @ResponseStatus(HttpStatus.OK)
    fun chat(
        @PathVariable knowledgeBaseId: String,
        @RequestBody request: ChatApiRequest
    ): ChatApiResponse {
        log.info { "Chat request for KB $knowledgeBaseId, conversation ${request.conversationId ?: "new"}" }

        val result = pipeline.send(
            RagChatCommand(
                conversationId = request.conversationId,
                message = request.message,
                knowledgeBaseId = knowledgeBaseId
            )
        )

        return ChatApiResponse(
            queryId = result.queryId,
            conversationId = result.conversationId,
            answer = result.answer,
            modelUsed = result.modelUsed,
            modelId = result.modelId,
            processingTimeMs = result.processingTimeMs
        )
    }

    @GetMapping("/chat/models")
    fun getAvailableModels(
        @PathVariable knowledgeBaseId: String
    ): AvailableModelsResponse {
        val result = pipeline.send(GetAvailableModelsQuery(privacyCompliantOnly = false))
        return AvailableModelsResponse(models = result.models, defaultModelId = result.defaultModelId)
    }

    @GetMapping("/chat/models/privacy")
    fun getPrivacyModels(
        @PathVariable knowledgeBaseId: String
    ): AvailableModelsResponse {
        val result = pipeline.send(GetAvailableModelsQuery(privacyCompliantOnly = true))
        return AvailableModelsResponse(models = result.models, defaultModelId = result.defaultModelId)
    }

    @PostMapping("/chat/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamChat(
        @PathVariable knowledgeBaseId: String,
        @RequestBody request: ChatApiRequest
    ): SseEmitter {
        log.info { "Streaming chat request for KB $knowledgeBaseId, conversation ${request.conversationId ?: "new"}" }

        val emitter = SseEmitter(STREAMING_TIMEOUT_MS)

        emitter.onCompletion {
            log.debug { "SSE connection completed for KB $knowledgeBaseId" }
        }
        emitter.onTimeout {
            log.warn { "SSE connection timed out for KB $knowledgeBaseId" }
        }
        emitter.onError { e ->
            log.error(e) { "SSE connection error for KB $knowledgeBaseId" }
        }

        pipeline.send(
            StreamingRagChatCommand(
                conversationId = request.conversationId,
                message = request.message,
                knowledgeBaseId = knowledgeBaseId,
                sseEmitter = emitter
            )
        )

        return emitter
    }

    companion object {
        private const val STREAMING_TIMEOUT_MS = 300_000L
    }
}

data class ChatApiRequest(
    val message: String,
    val conversationId: String? = null
)

data class ChatApiResponse(
    val queryId: UUID,
    val conversationId: String,
    val answer: String,
    val modelUsed: String,
    val modelId: String,
    val processingTimeMs: Long
)

data class AvailableModelsResponse(
    val models: List<LlmModelDto>,
    val defaultModelId: String
)
