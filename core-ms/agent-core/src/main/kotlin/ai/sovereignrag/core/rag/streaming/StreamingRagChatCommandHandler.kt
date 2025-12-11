package ai.sovereignrag.core.rag.streaming

import ai.sovereignrag.commons.knowledgebase.KnowledgeBaseRegistry
import ai.sovereignrag.commons.license.LicenseConfiguration
import ai.sovereignrag.core.rag.memory.ConversationMessageRepository
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.UUID

private val log = KotlinLogging.logger {}

@Component
class StreamingRagChatCommandHandler(
    private val streamingRagService: StreamingRagService,
    private val licenseConfiguration: LicenseConfiguration,
    private val knowledgeBaseRegistry: KnowledgeBaseRegistry,
    private val conversationMessageRepository: ConversationMessageRepository
) : Command.Handler<StreamingRagChatCommand, StreamingRagChatResult> {

    override fun handle(command: StreamingRagChatCommand): StreamingRagChatResult {
        val conversationId = command.conversationId ?: UUID.randomUUID().toString()

        val knowledgeBaseId = command.conversationId?.let { convId ->
            conversationMessageRepository.findFirstByConversationIdOrderBySequenceNumberAsc(convId)
                ?.knowledgeBaseId
                ?.toString()
        } ?: command.knowledgeBaseId
        ?: throw IllegalArgumentException("Knowledge base ID required for new conversations")

        val knowledgeBase = knowledgeBaseRegistry.getKnowledgeBase(knowledgeBaseId)
        val tier = licenseConfiguration.getLicenseInfo().tier

        log.info { "Handling streaming RAG chat for KB $knowledgeBaseId, conversation $conversationId" }

        val streamingRequest = StreamingRagChatRequest(
            knowledgeBaseId = knowledgeBaseId,
            conversationId = conversationId,
            query = command.message,
            tier = tier,
            modelId = knowledgeBase.llmModelId,
            systemPrompt = knowledgeBase.systemPrompt,
            requiresPrivacy = knowledgeBase.requiresEncryption,
            maxResults = knowledgeBase.maxRetrievalResults,
            minScore = knowledgeBase.minSimilarityScore,
            maxHistoryMessages = knowledgeBase.maxHistoryMessages
        )

        val session = streamingRagService.streamChat(streamingRequest)
        val emitter = command.sseEmitter

        sendMetadataEvent(emitter, session)
        subscribeToTokenStream(emitter, session)

        return StreamingRagChatResult(
            queryId = session.queryId,
            conversationId = session.conversationId,
            modelUsed = session.modelUsed,
            modelId = session.modelId
        )
    }

    private fun sendMetadataEvent(emitter: SseEmitter, session: StreamingChatSession) {
        runCatching {
            val metadata = StreamingMetadata(
                queryId = session.queryId.toString(),
                conversationId = session.conversationId,
                modelUsed = session.modelUsed,
                modelId = session.modelId
            )
            emitter.send(
                SseEmitter.event()
                    .name("metadata")
                    .data(metadata, MediaType.APPLICATION_JSON)
            )
        }.onFailure { e ->
            log.warn(e) { "Failed to send metadata event" }
        }
    }

    private fun subscribeToTokenStream(emitter: SseEmitter, session: StreamingChatSession) {
        val startTime = System.currentTimeMillis()

        session.tokenStream
            .onNext { token: String ->
                runCatching {
                    emitter.send(
                        SseEmitter.event()
                            .name("token")
                            .data(token)
                    )
                }.onFailure { e ->
                    log.warn(e) { "Failed to send token event" }
                }
            }
            .onComplete { response ->
                runCatching {
                    val processingTimeMs = System.currentTimeMillis() - startTime
                    val completion = StreamingCompletion(
                        fullResponse = response.content().text(),
                        processingTimeMs = processingTimeMs
                    )
                    emitter.send(
                        SseEmitter.event()
                            .name("complete")
                            .data(completion, MediaType.APPLICATION_JSON)
                    )
                    emitter.complete()
                    log.info { "Streaming completed for conversation ${session.conversationId} in ${processingTimeMs}ms" }
                }.onFailure { e ->
                    log.warn(e) { "Failed to send completion event" }
                    emitter.completeWithError(e)
                }
            }
            .onError { error: Throwable ->
                log.error(error) { "Error during streaming for conversation ${session.conversationId}" }
                runCatching {
                    emitter.send(
                        SseEmitter.event()
                            .name("error")
                            .data(StreamingError(error.message ?: "Unknown error"))
                    )
                    emitter.completeWithError(error)
                }
            }
            .start()
    }
}

data class StreamingMetadata(
    val queryId: String,
    val conversationId: String,
    val modelUsed: String,
    val modelId: String
)

data class StreamingCompletion(
    val fullResponse: String,
    val processingTimeMs: Long
)

data class StreamingError(
    val message: String
)
