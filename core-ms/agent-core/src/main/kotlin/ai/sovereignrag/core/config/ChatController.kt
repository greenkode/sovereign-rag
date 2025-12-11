package ai.sovereignrag.core.config

import ai.sovereignrag.auth.UserGatewayService
import ai.sovereignrag.commons.subscription.SubscriptionTier
import ai.sovereignrag.core.chat.ChatRequest
import ai.sovereignrag.core.chat.ChatService
import ai.sovereignrag.core.chat.ChatWithContextRequest
import ai.sovereignrag.core.chat.ConversationMessage
import ai.sovereignrag.core.chat.MessageRole
import ai.sovereignrag.core.chat.RetrievedContext
import ai.sovereignrag.core.llm.LlmModelSelectionService
import ai.sovereignrag.knowledgebase.configuration.dto.LlmModelDto
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/v1/knowledge-bases/{knowledgeBaseId}/chat")
class ChatController(
    private val chatService: ChatService,
    private val llmModelSelectionService: LlmModelSelectionService,
    private val userGatewayService: UserGatewayService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    fun chat(
        @PathVariable knowledgeBaseId: String,
        @RequestBody request: ChatApiRequest
    ): ChatApiResponse {
        val merchantId = getCurrentMerchantId()
        val tier = resolveTier(request.tier)

        log.info { "Chat request for KB $knowledgeBaseId from merchant $merchantId" }

        val chatRequest = ChatRequest(
            knowledgeBaseId = knowledgeBaseId,
            query = request.query,
            tier = tier,
            llmModelId = request.modelId,
            systemPrompt = request.systemPrompt,
            conversationHistory = request.history.map { ConversationMessage(MessageRole.valueOf(it.role), it.content) },
            requiresPrivacy = request.requiresPrivacy
        )

        val response = chatService.chat(chatRequest)

        return ChatApiResponse(
            queryId = response.queryId,
            answer = response.answer,
            modelUsed = response.modelUsed,
            modelId = response.modelId,
            tokenUsage = response.tokenUsage?.let {
                TokenUsageResponse(it.inputTokens, it.outputTokens, it.totalTokens)
            }
        )
    }

    @PostMapping("/rag")
    @ResponseStatus(HttpStatus.OK)
    fun chatWithContext(
        @PathVariable knowledgeBaseId: String,
        @RequestBody request: ChatWithContextApiRequest
    ): ChatApiResponse {
        val merchantId = getCurrentMerchantId()
        val tier = resolveTier(request.tier)

        log.info { "RAG chat request for KB $knowledgeBaseId with ${request.context.size} context items" }

        val chatRequest = ChatWithContextRequest(
            organizationId = merchantId,
            knowledgeBaseId = knowledgeBaseId,
            query = request.query,
            context = request.context.map { ctx ->
                RetrievedContext(
                    content = ctx.content,
                    sourceId = ctx.sourceId,
                    sourceName = ctx.sourceName,
                    score = ctx.score,
                    metadata = ctx.metadata
                )
            },
            tier = tier,
            llmModelId = request.modelId,
            conversationHistory = request.history.map { ConversationMessage(MessageRole.valueOf(it.role), it.content) },
            requiresPrivacy = request.requiresPrivacy,
            enableRemiEvaluation = request.enableRemiEvaluation
        )

        val response = chatService.chatWithContext(chatRequest)

        return ChatApiResponse(
            queryId = response.queryId,
            answer = response.answer,
            modelUsed = response.modelUsed,
            modelId = response.modelId,
            context = response.context?.map {
                ContextResponse(it.content, it.sourceId, it.sourceName, it.score, it.metadata)
            },
            tokenUsage = response.tokenUsage?.let {
                TokenUsageResponse(it.inputTokens, it.outputTokens, it.totalTokens)
            }
        )
    }

    @GetMapping("/models")
    fun getAvailableModels(
        @PathVariable knowledgeBaseId: String,
        @RequestParam(required = false) tier: String?
    ): AvailableModelsResponse {
        val resolvedTier = resolveTier(tier)

        log.debug { "Getting available models for tier $resolvedTier" }

        val models = llmModelSelectionService.getAvailableModels(resolvedTier)
        val defaultModel = llmModelSelectionService.getDefaultModel()

        return AvailableModelsResponse(
            models = models.map { it.toDto() },
            defaultModelId = defaultModel.id
        )
    }

    @GetMapping("/models/privacy")
    fun getPrivacyModels(
        @PathVariable knowledgeBaseId: String,
        @RequestParam(required = false) tier: String?
    ): AvailableModelsResponse {
        val resolvedTier = resolveTier(tier)

        log.debug { "Getting privacy-compliant models for tier $resolvedTier" }

        val models = llmModelSelectionService.getPrivacyCompliantModels(resolvedTier)
        val defaultModel = models.firstOrNull() ?: llmModelSelectionService.getDefaultModel()

        return AvailableModelsResponse(
            models = models.map { it.toDto() },
            defaultModelId = defaultModel.id
        )
    }

    private fun getCurrentMerchantId(): UUID =
        userGatewayService.getLoggedInMerchantId()
            ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Merchant not authenticated")

    private fun resolveTier(tierStr: String?): SubscriptionTier {
        return tierStr?.let {
            runCatching { SubscriptionTier.valueOf(it.uppercase()) }.getOrNull()
        } ?: SubscriptionTier.TRIAL
    }
}

data class ChatApiRequest(
    val query: String,
    val modelId: String? = null,
    val systemPrompt: String? = null,
    val history: List<MessageApiRequest> = emptyList(),
    val tier: String? = null,
    val requiresPrivacy: Boolean = false
)

data class ChatWithContextApiRequest(
    val query: String,
    val context: List<ContextApiRequest>,
    val modelId: String? = null,
    val history: List<MessageApiRequest> = emptyList(),
    val tier: String? = null,
    val requiresPrivacy: Boolean = false,
    val enableRemiEvaluation: Boolean = false
)

data class MessageApiRequest(
    val role: String,
    val content: String
)

data class ContextApiRequest(
    val content: String,
    val sourceId: String,
    val sourceName: String,
    val score: Double,
    val metadata: Map<String, String> = emptyMap()
)

data class ChatApiResponse(
    val queryId: UUID,
    val answer: String,
    val modelUsed: String,
    val modelId: String,
    val context: List<ContextResponse>? = null,
    val tokenUsage: TokenUsageResponse? = null
)

data class ContextResponse(
    val content: String,
    val sourceId: String,
    val sourceName: String,
    val score: Double,
    val metadata: Map<String, String> = emptyMap()
)

data class TokenUsageResponse(
    val inputTokens: Int,
    val outputTokens: Int,
    val totalTokens: Int
)

data class AvailableModelsResponse(
    val models: List<LlmModelDto>,
    val defaultModelId: String
)
