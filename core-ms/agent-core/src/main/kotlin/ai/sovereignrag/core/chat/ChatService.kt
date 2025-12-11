package ai.sovereignrag.core.chat

import ai.sovereignrag.commons.remi.RemiEvaluationGateway
import ai.sovereignrag.commons.remi.RemiEvaluationRequest
import ai.sovereignrag.commons.subscription.SubscriptionTier
import ai.sovereignrag.core.llm.LlmModelFactory
import ai.sovereignrag.core.llm.LlmModelSelectionService
import ai.sovereignrag.knowledgebase.configuration.domain.LlmModel
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.UserMessage
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.util.UUID

private val log = KotlinLogging.logger {}

@Service
class ChatService(
    private val llmModelSelectionService: LlmModelSelectionService,
    private val llmModelFactory: LlmModelFactory,
    private val remiEvaluationGateway: RemiEvaluationGateway?
) {

    fun chat(request: ChatRequest): ChatResponse {
        log.info { "Processing chat request for KB: ${request.knowledgeBaseId}" }

        val llmModel = resolveModel(request)
        val chatModel = llmModelFactory.createChatModel(llmModel)

        val messages = buildMessages(request)
        val response = chatModel.generate(messages)

        return ChatResponse(
            queryId = UUID.randomUUID(),
            answer = response.content().text(),
            modelUsed = llmModel.name,
            modelId = llmModel.id,
            tokenUsage = response.tokenUsage()?.let {
                TokenUsage(
                    inputTokens = it.inputTokenCount() ?: 0,
                    outputTokens = it.outputTokenCount() ?: 0,
                    totalTokens = it.totalTokenCount() ?: 0
                )
            }
        )
    }

    fun chatWithContext(request: ChatWithContextRequest): ChatResponse {
        log.info { "Processing chat with context for KB: ${request.knowledgeBaseId}" }

        val llmModel = resolveModel(
            ChatRequest(
                knowledgeBaseId = request.knowledgeBaseId,
                query = request.query,
                tier = request.tier,
                llmModelId = request.llmModelId,
                requiresPrivacy = request.requiresPrivacy
            )
        )
        val chatModel = llmModelFactory.createChatModel(llmModel)

        val messages = buildMessagesWithContext(request)
        val response = chatModel.generate(messages)
        val queryId = UUID.randomUUID()
        val answerText = response.content().text()

        val chatResponse = ChatResponse(
            queryId = queryId,
            answer = answerText,
            modelUsed = llmModel.name,
            modelId = llmModel.id,
            context = request.context,
            tokenUsage = response.tokenUsage()?.let {
                TokenUsage(
                    inputTokens = it.inputTokenCount() ?: 0,
                    outputTokens = it.outputTokenCount() ?: 0,
                    totalTokens = it.totalTokenCount() ?: 0
                )
            }
        )

        if (request.enableRemiEvaluation) {
            triggerRemiEvaluation(request, queryId, answerText, llmModel.name)
        }

        return chatResponse
    }

    private fun triggerRemiEvaluation(
        request: ChatWithContextRequest,
        queryId: UUID,
        generatedAnswer: String,
        modelUsed: String
    ) {
        remiEvaluationGateway?.let { gateway ->
            runCatching {
                val remiRequest = RemiEvaluationRequest(
                    organizationId = request.organizationId,
                    knowledgeBaseId = UUID.fromString(request.knowledgeBaseId),
                    queryId = queryId,
                    queryText = request.query,
                    generatedAnswer = generatedAnswer,
                    retrievedChunks = request.context.map { it.content },
                    modelUsed = modelUsed
                )

                gateway.evaluateAsync(remiRequest).thenAccept { result ->
                    log.info {
                        "REMI evaluation completed for query $queryId: " +
                        "overall=${result.overallScore}, hallucination=${result.hallucinationDetected}"
                    }
                }

                log.debug { "REMI evaluation triggered for query $queryId" }
            }.onFailure { e ->
                log.warn(e) { "Failed to trigger REMI evaluation for query $queryId" }
            }
        } ?: log.debug { "REMI evaluation gateway not available, skipping evaluation" }
    }

    private fun resolveModel(request: ChatRequest): LlmModel {
        return llmModelSelectionService.resolveModelForKnowledgeBase(
            llmModelId = request.llmModelId,
            tier = request.tier,
            requiresPrivacy = request.requiresPrivacy
        )
    }

    private fun buildMessages(request: ChatRequest): List<ChatMessage> {
        val messages = mutableListOf<ChatMessage>()

        request.systemPrompt?.let {
            messages.add(SystemMessage.from(it))
        }

        request.conversationHistory.forEach { message ->
            when (message.role) {
                MessageRole.USER -> messages.add(UserMessage.from(message.content))
                MessageRole.ASSISTANT -> messages.add(AiMessage.from(message.content))
                MessageRole.SYSTEM -> messages.add(SystemMessage.from(message.content))
            }
        }

        messages.add(UserMessage.from(request.query))

        return messages
    }

    private fun buildMessagesWithContext(request: ChatWithContextRequest): List<ChatMessage> {
        val messages = mutableListOf<ChatMessage>()

        val systemPrompt = buildRAGSystemPrompt(request.context)
        messages.add(SystemMessage.from(systemPrompt))

        request.conversationHistory.forEach { message ->
            when (message.role) {
                MessageRole.USER -> messages.add(UserMessage.from(message.content))
                MessageRole.ASSISTANT -> messages.add(AiMessage.from(message.content))
                MessageRole.SYSTEM -> messages.add(SystemMessage.from(message.content))
            }
        }

        messages.add(UserMessage.from(request.query))

        return messages
    }

    private fun buildRAGSystemPrompt(context: List<RetrievedContext>): String {
        val contextStr = context.mapIndexed { idx, ctx ->
            """
            |[Source ${idx + 1}]
            |${ctx.content}
            |---
            """.trimMargin()
        }.joinToString("\n")

        return """
            |You are a helpful AI assistant that answers questions based on the provided context.
            |Use ONLY the information from the context below to answer questions.
            |If the context doesn't contain enough information to answer, say so clearly.
            |Always cite your sources by referencing the source numbers.
            |
            |CONTEXT:
            |$contextStr
            |
            |Answer the user's question based on the context above.
        """.trimMargin()
    }
}

data class ChatRequest(
    val knowledgeBaseId: String,
    val query: String,
    val tier: SubscriptionTier,
    val llmModelId: String? = null,
    val systemPrompt: String? = null,
    val conversationHistory: List<ConversationMessage> = emptyList(),
    val requiresPrivacy: Boolean = false
)

data class ChatWithContextRequest(
    val organizationId: UUID,
    val knowledgeBaseId: String,
    val query: String,
    val context: List<RetrievedContext>,
    val tier: SubscriptionTier,
    val llmModelId: String? = null,
    val conversationHistory: List<ConversationMessage> = emptyList(),
    val requiresPrivacy: Boolean = false,
    val enableRemiEvaluation: Boolean = false
)

data class ChatResponse(
    val queryId: UUID,
    val answer: String,
    val modelUsed: String,
    val modelId: String,
    val context: List<RetrievedContext>? = null,
    val tokenUsage: TokenUsage? = null
)

data class TokenUsage(
    val inputTokens: Int,
    val outputTokens: Int,
    val totalTokens: Int
)

data class ConversationMessage(
    val role: MessageRole,
    val content: String
)

enum class MessageRole {
    USER, ASSISTANT, SYSTEM
}

data class RetrievedContext(
    val content: String,
    val sourceId: String,
    val sourceName: String,
    val score: Double,
    val metadata: Map<String, String> = emptyMap()
)
