package ai.sovereignrag.core.rag

import ai.sovereignrag.auth.UserGatewayService
import ai.sovereignrag.commons.remi.RemiEvaluationGateway
import ai.sovereignrag.commons.remi.RemiEvaluationRequest
import ai.sovereignrag.commons.subscription.SubscriptionTier
import ai.sovereignrag.core.llm.LlmModelFactory
import ai.sovereignrag.core.llm.LlmModelSelectionService
import ai.sovereignrag.core.rag.memory.JpaChatMemoryStore
import ai.sovereignrag.core.rag.retrieval.KnowledgeBaseRetrieverFactory
import ai.sovereignrag.knowledgebase.knowledgebase.KnowledgeBaseDefaults
import dev.langchain4j.memory.chat.MessageWindowChatMemory
import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.rag.content.retriever.ContentRetriever
import dev.langchain4j.service.AiServices
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private val log = KotlinLogging.logger {}

@Service
class RagService(
    private val llmModelSelectionService: LlmModelSelectionService,
    private val llmModelFactory: LlmModelFactory,
    private val chatMemoryStore: JpaChatMemoryStore,
    private val retrieverFactory: KnowledgeBaseRetrieverFactory,
    private val userGatewayService: UserGatewayService,
    private val remiEvaluationGateway: RemiEvaluationGateway?
) {
    private val assistantCache = ConcurrentHashMap<String, RagAssistant>()

    fun chat(request: RagChatRequest): RagChatResponse {
        val startTime = System.currentTimeMillis()
        val organizationId = userGatewayService.getLoggedInMerchantId()
            ?: throw IllegalStateException("Organization not found in security context")

        log.info { "Processing RAG chat for KB ${request.knowledgeBaseId}, conversation ${request.conversationId}" }

        chatMemoryStore.setContext(organizationId, UUID.fromString(request.knowledgeBaseId))

        val llmModel = llmModelSelectionService.resolveModelForKnowledgeBase(
            llmModelId = request.modelId,
            tier = request.tier,
            requiresPrivacy = request.requiresPrivacy
        )

        val chatModel = llmModelFactory.createChatModel(llmModel)
        val contentRetriever = retrieverFactory.createRetriever(
            knowledgeBaseId = request.knowledgeBaseId,
            maxResults = request.maxResults,
            minScore = request.minScore
        )

        val systemPrompt = request.systemPrompt ?: KnowledgeBaseDefaults.SYSTEM_PROMPT
        val cacheKey = "${request.knowledgeBaseId}:${llmModel.id}:${systemPrompt.hashCode()}"

        val assistant = getOrCreateAssistant(
            cacheKey = cacheKey,
            chatModel = chatModel,
            contentRetriever = contentRetriever,
            maxMessages = request.maxHistoryMessages,
            systemPrompt = systemPrompt
        )

        val answer = assistant.chat(request.conversationId, request.query)

        val queryId = UUID.randomUUID()
        val processingTimeMs = System.currentTimeMillis() - startTime

        log.info { "RAG chat completed in ${processingTimeMs}ms for conversation ${request.conversationId}" }

        if (request.enableRemiEvaluation) {
            triggerRemiEvaluation(organizationId, request.knowledgeBaseId, queryId, request.query, answer, llmModel.name)
        }

        return RagChatResponse(
            queryId = queryId,
            conversationId = request.conversationId,
            answer = answer,
            modelUsed = llmModel.name,
            modelId = llmModel.id,
            processingTimeMs = processingTimeMs
        )
    }

    fun deleteConversationsForKnowledgeBase(knowledgeBaseId: UUID) {
        log.info { "Deleting all conversations for KB: $knowledgeBaseId" }
        chatMemoryStore.deleteAllForKnowledgeBase(knowledgeBaseId)
        evictAssistantCache(knowledgeBaseId.toString())
    }

    fun evictAssistantCache(knowledgeBaseId: String) {
        assistantCache.keys
            .filter { it.startsWith("$knowledgeBaseId:") }
            .forEach { assistantCache.remove(it) }
        log.info { "Evicted assistant cache for KB: $knowledgeBaseId" }
    }

    private fun getOrCreateAssistant(
        cacheKey: String,
        chatModel: ChatLanguageModel,
        contentRetriever: ContentRetriever,
        maxMessages: Int,
        systemPrompt: String
    ): RagAssistant {
        return assistantCache.computeIfAbsent(cacheKey) {
            log.debug { "Creating new RagAssistant for: $cacheKey" }
            AiServices.builder(RagAssistant::class.java)
                .chatLanguageModel(chatModel)
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

    private fun triggerRemiEvaluation(
        organizationId: UUID,
        knowledgeBaseId: String,
        queryId: UUID,
        queryText: String,
        generatedAnswer: String,
        modelUsed: String
    ) {
        remiEvaluationGateway?.let { gateway ->
            runCatching {
                val remiRequest = RemiEvaluationRequest(
                    organizationId = organizationId,
                    knowledgeBaseId = UUID.fromString(knowledgeBaseId),
                    queryId = queryId,
                    queryText = queryText,
                    generatedAnswer = generatedAnswer,
                    retrievedChunks = emptyList(),
                    modelUsed = modelUsed
                )

                gateway.evaluateAsync(remiRequest).thenAccept { result ->
                    log.info {
                        "REMI evaluation completed for query $queryId: " +
                        "overall=${result.overallScore}, hallucination=${result.hallucinationDetected}"
                    }
                }
            }.onFailure { e ->
                log.warn(e) { "Failed to trigger REMI evaluation for query $queryId" }
            }
        }
    }
}

data class RagChatRequest(
    val knowledgeBaseId: String,
    val conversationId: String,
    val query: String,
    val tier: SubscriptionTier = SubscriptionTier.TRIAL,
    val modelId: String? = null,
    val systemPrompt: String? = null,
    val requiresPrivacy: Boolean = false,
    val enableRemiEvaluation: Boolean = false,
    val maxResults: Int = 5,
    val minScore: Double = 0.7,
    val maxHistoryMessages: Int = 20
)

data class RagChatResponse(
    val queryId: UUID,
    val conversationId: String,
    val answer: String,
    val modelUsed: String,
    val modelId: String,
    val processingTimeMs: Long
)
