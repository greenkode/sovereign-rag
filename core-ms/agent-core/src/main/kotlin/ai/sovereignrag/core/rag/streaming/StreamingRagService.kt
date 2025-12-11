package ai.sovereignrag.core.rag.streaming

import ai.sovereignrag.auth.UserGatewayService
import ai.sovereignrag.commons.subscription.SubscriptionTier
import ai.sovereignrag.core.llm.LlmModelFactory
import ai.sovereignrag.core.llm.LlmModelSelectionService
import ai.sovereignrag.core.rag.memory.JpaChatMemoryStore
import ai.sovereignrag.core.rag.retrieval.KnowledgeBaseRetrieverFactory
import ai.sovereignrag.knowledgebase.knowledgebase.KnowledgeBaseDefaults
import dev.langchain4j.memory.chat.MessageWindowChatMemory
import dev.langchain4j.model.chat.StreamingChatLanguageModel
import dev.langchain4j.rag.content.retriever.ContentRetriever
import dev.langchain4j.service.AiServices
import dev.langchain4j.service.TokenStream
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private val log = KotlinLogging.logger {}

@Service
class StreamingRagService(
    private val llmModelSelectionService: LlmModelSelectionService,
    private val llmModelFactory: LlmModelFactory,
    private val chatMemoryStore: JpaChatMemoryStore,
    private val retrieverFactory: KnowledgeBaseRetrieverFactory,
    private val userGatewayService: UserGatewayService
) {
    private val assistantCache = ConcurrentHashMap<String, StreamingRagAssistant>()

    fun streamChat(request: StreamingRagChatRequest): StreamingChatSession {
        val organizationId = userGatewayService.getLoggedInMerchantId()
            ?: throw IllegalStateException("Organization not found in security context")

        log.info { "Processing streaming RAG chat for KB ${request.knowledgeBaseId}, conversation ${request.conversationId}" }

        chatMemoryStore.setContext(organizationId, UUID.fromString(request.knowledgeBaseId))

        val llmModel = llmModelSelectionService.resolveModelForKnowledgeBase(
            llmModelId = request.modelId,
            tier = request.tier,
            requiresPrivacy = request.requiresPrivacy
        )

        val streamingChatModel = llmModelFactory.createStreamingChatModel(llmModel)
        val contentRetriever = retrieverFactory.createRetriever(
            knowledgeBaseId = request.knowledgeBaseId,
            maxResults = request.maxResults,
            minScore = request.minScore
        )

        val systemPrompt = request.systemPrompt ?: KnowledgeBaseDefaults.SYSTEM_PROMPT
        val cacheKey = "streaming:${request.knowledgeBaseId}:${llmModel.id}:${systemPrompt.hashCode()}"

        val assistant = getOrCreateAssistant(
            cacheKey = cacheKey,
            streamingChatModel = streamingChatModel,
            contentRetriever = contentRetriever,
            maxMessages = request.maxHistoryMessages,
            systemPrompt = systemPrompt
        )

        val queryId = UUID.randomUUID()
        val tokenStream = assistant.chat(request.conversationId, request.query)

        log.info { "Starting streaming chat for conversation ${request.conversationId}, queryId: $queryId" }

        return StreamingChatSession(
            queryId = queryId,
            conversationId = request.conversationId,
            modelUsed = llmModel.name,
            modelId = llmModel.id,
            tokenStream = tokenStream
        )
    }

    fun evictAssistantCache(knowledgeBaseId: String) {
        assistantCache.keys
            .filter { it.contains(":$knowledgeBaseId:") }
            .forEach { assistantCache.remove(it) }
        log.info { "Evicted streaming assistant cache for KB: $knowledgeBaseId" }
    }

    private fun getOrCreateAssistant(
        cacheKey: String,
        streamingChatModel: StreamingChatLanguageModel,
        contentRetriever: ContentRetriever,
        maxMessages: Int,
        systemPrompt: String
    ): StreamingRagAssistant {
        return assistantCache.computeIfAbsent(cacheKey) {
            log.debug { "Creating new StreamingRagAssistant for: $cacheKey" }
            AiServices.builder(StreamingRagAssistant::class.java)
                .streamingChatLanguageModel(streamingChatModel)
                .contentRetriever(contentRetriever)
                .systemMessageProvider { systemPrompt }
                .chatMemoryProvider { memoryId ->
                    MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .maxMessages(maxMessages)
                        .chatMemoryStore(chatMemoryStore)
                        .build()
                }
                .build()
        }
    }
}

data class StreamingRagChatRequest(
    val knowledgeBaseId: String,
    val conversationId: String,
    val query: String,
    val tier: SubscriptionTier = SubscriptionTier.TRIAL,
    val modelId: String? = null,
    val systemPrompt: String? = null,
    val requiresPrivacy: Boolean = false,
    val maxResults: Int = 5,
    val minScore: Double = 0.7,
    val maxHistoryMessages: Int = 20
)

data class StreamingChatSession(
    val queryId: UUID,
    val conversationId: String,
    val modelUsed: String,
    val modelId: String,
    val tokenStream: TokenStream
)
