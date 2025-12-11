package ai.sovereignrag.core.rag.command

import ai.sovereignrag.commons.knowledgebase.KnowledgeBaseInfo
import ai.sovereignrag.commons.knowledgebase.KnowledgeBaseRegistry
import ai.sovereignrag.commons.knowledgebase.KnowledgeBaseStatus
import ai.sovereignrag.commons.license.LicenseConfiguration
import ai.sovereignrag.commons.license.LicenseInfo
import ai.sovereignrag.commons.subscription.SubscriptionTier
import ai.sovereignrag.core.rag.RagChatRequest
import ai.sovereignrag.core.rag.RagChatResponse
import ai.sovereignrag.core.rag.RagService
import ai.sovereignrag.core.rag.memory.ConversationMessage
import ai.sovereignrag.core.rag.memory.ConversationMessageRepository
import ai.sovereignrag.core.rag.memory.MessageType
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RagChatCommandHandlerTest {

    private val ragService: RagService = mockk()
    private val licenseConfiguration: LicenseConfiguration = mockk()
    private val knowledgeBaseRegistry: KnowledgeBaseRegistry = mockk()
    private val conversationMessageRepository: ConversationMessageRepository = mockk()

    private lateinit var handler: RagChatCommandHandler

    private val organizationId = UUID.randomUUID()
    private val knowledgeBaseId = UUID.randomUUID().toString()

    @BeforeEach
    fun setup() {
        handler = RagChatCommandHandler(
            ragService,
            licenseConfiguration,
            knowledgeBaseRegistry,
            conversationMessageRepository
        )

        every { licenseConfiguration.getLicenseInfo() } returns createLicenseInfo(SubscriptionTier.PROFESSIONAL)
    }

    @Test
    fun `should create new conversation when conversationId is null`() {
        val command = RagChatCommand(
            conversationId = null,
            message = "Hello, how can you help?",
            knowledgeBaseId = knowledgeBaseId
        )

        val knowledgeBase = createMockKnowledgeBase()
        val ragRequestSlot = slot<RagChatRequest>()
        val queryId = UUID.randomUUID()

        every { knowledgeBaseRegistry.getKnowledgeBase(knowledgeBaseId) } returns knowledgeBase
        every { ragService.chat(capture(ragRequestSlot)) } returns RagChatResponse(
            queryId = queryId,
            conversationId = "generated-conversation-id",
            answer = "I can help you with various tasks.",
            modelUsed = "GPT-4",
            modelId = "gpt-4",
            processingTimeMs = 500
        )

        val result = handler.handle(command)

        assertNotNull(result)
        assertEquals(queryId, result.queryId)
        assertEquals("I can help you with various tasks.", result.answer)
        assertEquals("GPT-4", result.modelUsed)

        val capturedRequest = ragRequestSlot.captured
        assertEquals(knowledgeBaseId, capturedRequest.knowledgeBaseId)
        assertEquals("Hello, how can you help?", capturedRequest.query)
        assertNotNull(capturedRequest.conversationId)

        verify { knowledgeBaseRegistry.getKnowledgeBase(knowledgeBaseId) }
        verify { ragService.chat(any()) }
    }

    @Test
    fun `should use existing conversation and derive knowledge base from it`() {
        val existingConversationId = "existing-conversation-123"
        val command = RagChatCommand(
            conversationId = existingConversationId,
            message = "What about the next step?",
            knowledgeBaseId = null
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
        val queryId = UUID.randomUUID()

        every { conversationMessageRepository.findFirstByConversationIdOrderBySequenceNumberAsc(existingConversationId) } returns conversationMessage
        every { knowledgeBaseRegistry.getKnowledgeBase(knowledgeBaseId) } returns knowledgeBase
        every { ragService.chat(any()) } returns RagChatResponse(
            queryId = queryId,
            conversationId = existingConversationId,
            answer = "The next step is...",
            modelUsed = "GPT-4",
            modelId = "gpt-4",
            processingTimeMs = 400
        )

        val result = handler.handle(command)

        assertNotNull(result)
        assertEquals(existingConversationId, result.conversationId)
        assertEquals("The next step is...", result.answer)

        verify { conversationMessageRepository.findFirstByConversationIdOrderBySequenceNumberAsc(existingConversationId) }
        verify { knowledgeBaseRegistry.getKnowledgeBase(knowledgeBaseId) }
    }

    @Test
    fun `should throw exception when no knowledge base ID provided for new conversation`() {
        val command = RagChatCommand(
            conversationId = null,
            message = "Hello",
            knowledgeBaseId = null
        )

        assertThrows<IllegalArgumentException> {
            handler.handle(command)
        }
    }

    @Test
    fun `should throw exception when conversation not found and no knowledge base ID provided`() {
        val command = RagChatCommand(
            conversationId = "non-existent-conversation",
            message = "Hello",
            knowledgeBaseId = null
        )

        every { conversationMessageRepository.findFirstByConversationIdOrderBySequenceNumberAsc("non-existent-conversation") } returns null

        assertThrows<IllegalArgumentException> {
            handler.handle(command)
        }
    }

    @Test
    fun `should use knowledge base settings for chat request`() {
        val command = RagChatCommand(
            conversationId = null,
            message = "Test message",
            knowledgeBaseId = knowledgeBaseId
        )

        val knowledgeBase = createMockKnowledgeBase(
            maxRetrievalResults = 10,
            minSimilarityScore = 0.8,
            maxHistoryMessages = 30,
            enableRemiEvaluation = true,
            systemPrompt = "You are a helpful assistant."
        )

        val ragRequestSlot = slot<RagChatRequest>()

        every { knowledgeBaseRegistry.getKnowledgeBase(knowledgeBaseId) } returns knowledgeBase
        every { ragService.chat(capture(ragRequestSlot)) } returns RagChatResponse(
            queryId = UUID.randomUUID(),
            conversationId = "test-conv",
            answer = "Response",
            modelUsed = "GPT-4",
            modelId = "gpt-4",
            processingTimeMs = 100
        )

        handler.handle(command)

        val capturedRequest = ragRequestSlot.captured
        assertEquals(10, capturedRequest.maxResults)
        assertEquals(0.8, capturedRequest.minScore)
        assertEquals(30, capturedRequest.maxHistoryMessages)
        assertEquals(true, capturedRequest.enableRemiEvaluation)
        assertEquals("You are a helpful assistant.", capturedRequest.systemPrompt)
    }

    @Test
    fun `should pass subscription tier to chat request`() {
        val command = RagChatCommand(
            conversationId = null,
            message = "Test",
            knowledgeBaseId = knowledgeBaseId
        )

        every { licenseConfiguration.getLicenseInfo() } returns createLicenseInfo(SubscriptionTier.ENTERPRISE)

        val knowledgeBase = createMockKnowledgeBase()
        val ragRequestSlot = slot<RagChatRequest>()

        every { knowledgeBaseRegistry.getKnowledgeBase(knowledgeBaseId) } returns knowledgeBase
        every { ragService.chat(capture(ragRequestSlot)) } returns RagChatResponse(
            queryId = UUID.randomUUID(),
            conversationId = "test",
            answer = "Response",
            modelUsed = "GPT-4",
            modelId = "gpt-4",
            processingTimeMs = 100
        )

        handler.handle(command)

        assertEquals(SubscriptionTier.ENTERPRISE, ragRequestSlot.captured.tier)
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
}
