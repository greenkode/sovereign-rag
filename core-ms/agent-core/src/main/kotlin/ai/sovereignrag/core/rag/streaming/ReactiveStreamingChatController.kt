package ai.sovereignrag.core.rag.streaming

import ai.sovereignrag.commons.knowledgebase.KnowledgeBaseRegistry
import ai.sovereignrag.commons.license.LicenseConfiguration
import ai.sovereignrag.core.rag.memory.ConversationMessageRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import reactor.core.scheduler.Schedulers
import java.util.UUID

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/v1/knowledge-bases/{knowledgeBaseId}/chat")
class ReactiveStreamingChatController(
    private val streamingRagService: StreamingRagService,
    private val licenseConfiguration: LicenseConfiguration,
    private val knowledgeBaseRegistry: KnowledgeBaseRegistry,
    private val conversationMessageRepository: ConversationMessageRepository
) {

    @PostMapping("/stream/reactive", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamChatReactive(
        @PathVariable knowledgeBaseId: String,
        @RequestBody request: ReactiveStreamRequest
    ): Flux<ServerSentEvent<StreamEvent>> {
        log.info { "Reactive streaming chat request for KB $knowledgeBaseId, conversation ${request.conversationId ?: "new"}" }

        return Flux.create<ServerSentEvent<StreamEvent>> { sink ->
            handleStreamingChat(knowledgeBaseId, request, sink)
        }
            .subscribeOn(Schedulers.boundedElastic())
            .doOnCancel { log.debug { "Client cancelled streaming request" } }
            .doOnError { e -> log.error(e) { "Error in reactive stream" } }
            .doOnComplete { log.debug { "Reactive stream completed" } }
    }

    private fun handleStreamingChat(
        knowledgeBaseId: String,
        request: ReactiveStreamRequest,
        sink: FluxSink<ServerSentEvent<StreamEvent>>
    ) {
        val startTime = System.currentTimeMillis()

        runCatching {
            val conversationId = request.conversationId ?: UUID.randomUUID().toString()

            val resolvedKnowledgeBaseId = request.conversationId?.let { convId ->
                conversationMessageRepository.findFirstByConversationIdOrderBySequenceNumberAsc(convId)
                    ?.knowledgeBaseId
                    ?.toString()
            } ?: knowledgeBaseId

            val knowledgeBase = knowledgeBaseRegistry.getKnowledgeBase(resolvedKnowledgeBaseId)
            val tier = licenseConfiguration.getLicenseInfo().tier

            val streamingRequest = StreamingRagChatRequest(
                knowledgeBaseId = resolvedKnowledgeBaseId,
                conversationId = conversationId,
                query = request.message,
                tier = tier,
                modelId = knowledgeBase.llmModelId,
                systemPrompt = knowledgeBase.systemPrompt,
                requiresPrivacy = knowledgeBase.requiresEncryption,
                maxResults = knowledgeBase.maxRetrievalResults,
                minScore = knowledgeBase.minSimilarityScore,
                maxHistoryMessages = knowledgeBase.maxHistoryMessages
            )

            val session = streamingRagService.streamChat(streamingRequest)

            sink.next(
                ServerSentEvent.builder<StreamEvent>()
                    .event("metadata")
                    .data(
                        StreamEvent.Metadata(
                            queryId = session.queryId.toString(),
                            conversationId = session.conversationId,
                            modelUsed = session.modelUsed,
                            modelId = session.modelId
                        )
                    )
                    .build()
            )

            session.tokenStream
                .onNext { token: String ->
                    sink.next(
                        ServerSentEvent.builder<StreamEvent>()
                            .event("token")
                            .data(StreamEvent.Token(token))
                            .build()
                    )
                }
                .onComplete { response ->
                    val processingTimeMs = System.currentTimeMillis() - startTime
                    sink.next(
                        ServerSentEvent.builder<StreamEvent>()
                            .event("complete")
                            .data(
                                StreamEvent.Complete(
                                    fullResponse = response.content().text(),
                                    processingTimeMs = processingTimeMs
                                )
                            )
                            .build()
                    )
                    sink.complete()
                    log.info { "Reactive streaming completed for conversation ${session.conversationId} in ${processingTimeMs}ms" }
                }
                .onError { error: Throwable ->
                    log.error(error) { "Error during reactive streaming" }
                    sink.next(
                        ServerSentEvent.builder<StreamEvent>()
                            .event("error")
                            .data(StreamEvent.Error(error.message ?: "Unknown error"))
                            .build()
                    )
                    sink.error(error)
                }
                .start()
        }.onFailure { e ->
            log.error(e) { "Failed to start reactive streaming" }
            sink.next(
                ServerSentEvent.builder<StreamEvent>()
                    .event("error")
                    .data(StreamEvent.Error(e.message ?: "Failed to start streaming"))
                    .build()
            )
            sink.error(e)
        }
    }
}

data class ReactiveStreamRequest(
    val message: String,
    val conversationId: String? = null
)

sealed class StreamEvent {
    data class Metadata(
        val queryId: String,
        val conversationId: String,
        val modelUsed: String,
        val modelId: String
    ) : StreamEvent()

    data class Token(
        val content: String
    ) : StreamEvent()

    data class Complete(
        val fullResponse: String,
        val processingTimeMs: Long
    ) : StreamEvent()

    data class Error(
        val message: String
    ) : StreamEvent()
}
