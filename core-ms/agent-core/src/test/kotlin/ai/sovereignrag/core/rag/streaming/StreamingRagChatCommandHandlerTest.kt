package ai.sovereignrag.core.rag.streaming

import ai.sovereignrag.commons.knowledgebase.KnowledgeBaseInfo
import ai.sovereignrag.commons.knowledgebase.KnowledgeBaseRegistry
import ai.sovereignrag.commons.knowledgebase.KnowledgeBaseStatus
import ai.sovereignrag.commons.license.LicenseConfiguration
import ai.sovereignrag.commons.license.LicenseInfo
import ai.sovereignrag.commons.subscription.SubscriptionTier
import ai.sovereignrag.core.rag.memory.ConversationMessage
import ai.sovereignrag.core.rag.memory.ConversationMessageRepository
import ai.sovereignrag.core.rag.memory.MessageType
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.model.output.Response
import dev.langchain4j.service.TokenStream
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.time.Instant
import java.util.UUID
import java.util.function.Consumer
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class StreamingRagChatCommandHandlerTest {

    private val streamingRagService: StreamingRagService = mockk()
    private val licenseConfiguration: LicenseConfiguration = mockk()
    private val knowledgeBaseRegistry: KnowledgeBaseRegistry = mockk()
    private val conversationMessageRepository: ConversationMessageRepository = mockk()

    private lateinit var handler: StreamingRagChatCommandHandler

    private val organizationId = UUID.randomUUID()
    private val knowledgeBaseId = UUID.randomUUID().toString()

    @BeforeEach
    fun setup() {
        handler = StreamingRagChatCommandHandler(
            streamingRagService,
            licenseConfiguration,
            knowledgeBaseRegistry,
            conversationMessageRepository
        )

        every { licenseConfiguration.getLicenseInfo() } returns createLicenseInfo(SubscriptionTier.PROFESSIONAL)
    }

    @Test
    fun `should start streaming for new conversation`() {
        val sseEmitter = mockk<SseEmitter>(relaxed = true)
        val command = StreamingRagChatCommand(
            conversationId = null,
            message = "Tell me a story",
            knowledgeBaseId = knowledgeBaseId,
            sseEmitter = sseEmitter
        )

        val knowledgeBase = createMockKnowledgeBase()
        val mockTokenStream = createMockTokenStream()
        val queryId = UUID.randomUUID()

        every { knowledgeBaseRegistry.getKnowledgeBase(knowledgeBaseId) } returns knowledgeBase
        every { streamingRagService.streamChat(any()) } returns StreamingChatSession(
            queryId = queryId,
            conversationId = "generated-conv-id",
            modelUsed = "GPT-4",
            modelId = "gpt-4",
            tokenStream = mockTokenStream
        )

        val result = handler.handle(command)

        assertNotNull(result)
        assertEquals(queryId, result.queryId)
        assertEquals("GPT-4", result.modelUsed)
        assertEquals("gpt-4", result.modelId)

        verify { knowledgeBaseRegistry.getKnowledgeBase(knowledgeBaseId) }
        verify { streamingRagService.streamChat(any()) }
    }

    @Test
    fun `should derive knowledge base from existing conversation`() {
        val existingConversationId = "existing-conv-456"
        val sseEmitter = mockk<SseEmitter>(relaxed = true)
        val command = StreamingRagChatCommand(
            conversationId = existingConversationId,
            message = "Continue the story",
            knowledgeBaseId = null,
            sseEmitter = sseEmitter
        )

        val conversationMessage = ConversationMessage(
            conversationId = existingConversationId,
            organizationId = organizationId,
            knowledgeBaseId = UUID.fromString(knowledgeBaseId),
            messageType = MessageType.USER,
            content = "Previous message",
            sequenceNumber = 0,
            createdAt = Instant.now()
        )

        val knowledgeBase = createMockKnowledgeBase()
        val mockTokenStream = createMockTokenStream()

        every { conversationMessageRepository.findFirstByConversationIdOrderBySequenceNumberAsc(existingConversationId) } returns conversationMessage
        every { knowledgeBaseRegistry.getKnowledgeBase(knowledgeBaseId) } returns knowledgeBase
        every { streamingRagService.streamChat(any()) } returns StreamingChatSession(
            queryId = UUID.randomUUID(),
            conversationId = existingConversationId,
            modelUsed = "GPT-4",
            modelId = "gpt-4",
            tokenStream = mockTokenStream
        )

        val result = handler.handle(command)

        assertEquals(existingConversationId, result.conversationId)
        verify { conversationMessageRepository.findFirstByConversationIdOrderBySequenceNumberAsc(existingConversationId) }
    }

    @Test
    fun `should throw exception when no knowledge base ID for new conversation`() {
        val sseEmitter = mockk<SseEmitter>(relaxed = true)
        val command = StreamingRagChatCommand(
            conversationId = null,
            message = "Hello",
            knowledgeBaseId = null,
            sseEmitter = sseEmitter
        )

        assertThrows<IllegalArgumentException> {
            handler.handle(command)
        }
    }

    @Test
    fun `should use knowledge base settings for streaming request`() {
        val sseEmitter = mockk<SseEmitter>(relaxed = true)
        val command = StreamingRagChatCommand(
            conversationId = null,
            message = "Test",
            knowledgeBaseId = knowledgeBaseId,
            sseEmitter = sseEmitter
        )

        val knowledgeBase = createMockKnowledgeBase(
            maxRetrievalResults = 15,
            minSimilarityScore = 0.9,
            maxHistoryMessages = 50,
            systemPrompt = "Custom prompt"
        )

        val streamingRequestSlot = slot<StreamingRagChatRequest>()
        val mockTokenStream = createMockTokenStream()

        every { knowledgeBaseRegistry.getKnowledgeBase(knowledgeBaseId) } returns knowledgeBase
        every { streamingRagService.streamChat(capture(streamingRequestSlot)) } returns StreamingChatSession(
            queryId = UUID.randomUUID(),
            conversationId = "test",
            modelUsed = "GPT-4",
            modelId = "gpt-4",
            tokenStream = mockTokenStream
        )

        handler.handle(command)

        val capturedRequest = streamingRequestSlot.captured
        assertEquals(15, capturedRequest.maxResults)
        assertEquals(0.9, capturedRequest.minScore)
        assertEquals(50, capturedRequest.maxHistoryMessages)
        assertEquals("Custom prompt", capturedRequest.systemPrompt)
    }

    @Test
    fun `should send metadata event via SSE emitter`() {
        val sseEmitter = mockk<SseEmitter>(relaxed = true)
        val command = StreamingRagChatCommand(
            conversationId = null,
            message = "Test",
            knowledgeBaseId = knowledgeBaseId,
            sseEmitter = sseEmitter
        )

        val knowledgeBase = createMockKnowledgeBase()
        val mockTokenStream = createMockTokenStream()
        val queryId = UUID.randomUUID()

        every { knowledgeBaseRegistry.getKnowledgeBase(knowledgeBaseId) } returns knowledgeBase
        every { streamingRagService.streamChat(any()) } returns StreamingChatSession(
            queryId = queryId,
            conversationId = "test-conv",
            modelUsed = "GPT-4",
            modelId = "gpt-4",
            tokenStream = mockTokenStream
        )

        handler.handle(command)

        verify { sseEmitter.send(any<SseEmitter.SseEventBuilder>()) }
    }

    private fun createLicenseInfo(tier: SubscriptionTier): LicenseInfo = LicenseInfo(
        licenseKey = "test-license-key",
        customerId = "customer-123",
        customerName = "Test Customer",
        tier = tier,
        maxTokensPerMonth = 1000000,
        maxKnowledgeBases = 10,
        features = emptySet(),
        issuedAt = Instant.now(),
        expiresAt = Instant.now().plusSeconds(86400),
        isValid = true
    )

    private fun createMockKnowledgeBase(
        maxRetrievalResults: Int = 5,
        minSimilarityScore: Double = 0.7,
        maxHistoryMessages: Int = 20,
        enableRemiEvaluation: Boolean = false,
        systemPrompt: String? = null,
        llmModelId: String? = null
    ): KnowledgeBaseInfo {
        val mock = mockk<KnowledgeBaseInfo>()
        every { mock.id } returns knowledgeBaseId
        every { mock.organizationId } returns organizationId
        every { mock.schemaName } returns "kb_test"
        every { mock.regionCode } returns "eu-west"
        every { mock.status } returns KnowledgeBaseStatus.ACTIVE
        every { mock.systemPrompt } returns systemPrompt
        every { mock.llmModelId } returns llmModelId
        every { mock.embeddingModelId } returns null
        every { mock.requiresEncryption } returns false
        every { mock.maxRetrievalResults } returns maxRetrievalResults
        every { mock.minSimilarityScore } returns minSimilarityScore
        every { mock.maxHistoryMessages } returns maxHistoryMessages
        every { mock.enableRemiEvaluation } returns enableRemiEvaluation
        every { mock.oauthClientId } returns null
        every { mock.apiKeyHash } returns null
        return mock
    }

    private fun createMockTokenStream(): TokenStream {
        return object : TokenStream {
            private var nextHandler: Consumer<String>? = null
            private var completeHandler: Consumer<Response<AiMessage>>? = null
            private var errorHandler: Consumer<Throwable>? = null

            override fun onRetrieved(contentHandler: Consumer<MutableList<dev.langchain4j.rag.content.Content>>?): TokenStream = this

            override fun onNext(tokenHandler: Consumer<String>): TokenStream {
                this.nextHandler = tokenHandler
                return this
            }

            override fun onComplete(completionHandler: Consumer<Response<AiMessage>>): TokenStream {
                this.completeHandler = completionHandler
                return this
            }

            override fun onError(errorHandler: Consumer<Throwable>): TokenStream {
                this.errorHandler = errorHandler
                return this
            }

            override fun ignoreErrors(): TokenStream = this

            override fun start() {
                nextHandler?.accept("Hello")
                nextHandler?.accept(" World")
                completeHandler?.accept(Response.from(AiMessage.from("Hello World")))
            }
        }
    }
}
