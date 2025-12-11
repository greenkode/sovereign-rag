package ai.sovereignrag.core.rag.query

import ai.sovereignrag.commons.license.LicenseConfiguration
import ai.sovereignrag.commons.license.LicenseInfo
import ai.sovereignrag.commons.subscription.SubscriptionTier
import ai.sovereignrag.core.llm.LlmModelSelectionService
import ai.sovereignrag.knowledgebase.configuration.domain.LlmModel
import ai.sovereignrag.knowledgebase.configuration.domain.LlmPrivacyLevel
import ai.sovereignrag.knowledgebase.configuration.domain.LlmProviderType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GetAvailableModelsQueryHandlerTest {

    private val llmModelSelectionService: LlmModelSelectionService = mockk()
    private val licenseConfiguration: LicenseConfiguration = mockk()

    private lateinit var handler: GetAvailableModelsQueryHandler

    @BeforeEach
    fun setup() {
        handler = GetAvailableModelsQueryHandler(
            llmModelSelectionService,
            licenseConfiguration
        )
    }

    @Test
    fun `should return all available models for tier`() {
        val query = GetAvailableModelsQuery(privacyCompliantOnly = false)

        every { licenseConfiguration.getLicenseInfo() } returns createLicenseInfo(SubscriptionTier.PROFESSIONAL)

        val models = listOf(
            createLlmModel("gpt-4", "GPT-4", "openai", LlmProviderType.CLOUD, isDefault = true),
            createLlmModel("claude-3", "Claude 3", "anthropic", LlmProviderType.CLOUD),
            createLlmModel("llama2", "Llama 2", "ollama", LlmProviderType.LOCAL)
        )
        val defaultModel = models.first()

        every { llmModelSelectionService.getAvailableModels(SubscriptionTier.PROFESSIONAL) } returns models
        every { llmModelSelectionService.getDefaultModel() } returns defaultModel

        val result = handler.handle(query)

        assertNotNull(result)
        assertEquals(3, result.models.size)
        assertEquals("gpt-4", result.defaultModelId)

        verify { llmModelSelectionService.getAvailableModels(SubscriptionTier.PROFESSIONAL) }
        verify { llmModelSelectionService.getDefaultModel() }
    }

    @Test
    fun `should return only privacy compliant models when requested`() {
        val query = GetAvailableModelsQuery(privacyCompliantOnly = true)

        every { licenseConfiguration.getLicenseInfo() } returns createLicenseInfo(SubscriptionTier.ENTERPRISE)

        val localModels = listOf(
            createLlmModel("llama2", "Llama 2", "ollama", LlmProviderType.LOCAL, privacyLevel = LlmPrivacyLevel.MAXIMUM),
            createLlmModel("mistral", "Mistral", "ollama", LlmProviderType.LOCAL, privacyLevel = LlmPrivacyLevel.MAXIMUM)
        )

        every { llmModelSelectionService.getPrivacyCompliantModels(SubscriptionTier.ENTERPRISE) } returns localModels
        every { llmModelSelectionService.getDefaultModel() } returns localModels.first()

        val result = handler.handle(query)

        assertNotNull(result)
        assertEquals(2, result.models.size)
        assertEquals("llama2", result.defaultModelId)

        verify { llmModelSelectionService.getPrivacyCompliantModels(SubscriptionTier.ENTERPRISE) }
        verify(exactly = 0) { llmModelSelectionService.getAvailableModels(any()) }
    }

    @Test
    fun `should use first privacy model as default when privacy mode enabled`() {
        val query = GetAvailableModelsQuery(privacyCompliantOnly = true)

        every { licenseConfiguration.getLicenseInfo() } returns createLicenseInfo(SubscriptionTier.PROFESSIONAL)

        val localModels = listOf(
            createLlmModel("mistral-local", "Mistral Local", "ollama", LlmProviderType.LOCAL, privacyLevel = LlmPrivacyLevel.MAXIMUM)
        )

        every { llmModelSelectionService.getPrivacyCompliantModels(SubscriptionTier.PROFESSIONAL) } returns localModels
        every { llmModelSelectionService.getDefaultModel() } returns createLlmModel("gpt-4", "GPT-4", "openai", LlmProviderType.CLOUD)

        val result = handler.handle(query)

        assertEquals("mistral-local", result.defaultModelId)
    }

    @Test
    fun `should fallback to default model when no privacy models available`() {
        val query = GetAvailableModelsQuery(privacyCompliantOnly = true)

        every { licenseConfiguration.getLicenseInfo() } returns createLicenseInfo(SubscriptionTier.TRIAL)

        val defaultModel = createLlmModel("gpt-4", "GPT-4", "openai", LlmProviderType.CLOUD, isDefault = true)

        every { llmModelSelectionService.getPrivacyCompliantModels(SubscriptionTier.TRIAL) } returns emptyList()
        every { llmModelSelectionService.getDefaultModel() } returns defaultModel

        val result = handler.handle(query)

        assertTrue(result.models.isEmpty())
        assertEquals("gpt-4", result.defaultModelId)
    }

    @Test
    fun `should convert models to DTOs`() {
        val query = GetAvailableModelsQuery(privacyCompliantOnly = false)

        every { licenseConfiguration.getLicenseInfo() } returns createLicenseInfo(SubscriptionTier.PROFESSIONAL)

        val model = createLlmModel(
            id = "test-model",
            name = "Test Model",
            provider = "openai",
            providerType = LlmProviderType.CLOUD,
            isDefault = true,
            maxTokens = 8192,
            contextWindow = 16384
        )

        every { llmModelSelectionService.getAvailableModels(any()) } returns listOf(model)
        every { llmModelSelectionService.getDefaultModel() } returns model

        val result = handler.handle(query)

        val dto = result.models.first()
        assertEquals("test-model", dto.id)
        assertEquals("Test Model", dto.name)
        assertEquals("openai", dto.provider)
        assertEquals(8192, dto.maxTokens)
        assertEquals(16384, dto.contextWindow)
    }

    @Test
    fun `should handle different subscription tiers`() {
        listOf(
            SubscriptionTier.TRIAL,
            SubscriptionTier.STARTER,
            SubscriptionTier.PROFESSIONAL,
            SubscriptionTier.ENTERPRISE
        ).forEach { tier ->
            val query = GetAvailableModelsQuery(privacyCompliantOnly = false)

            every { licenseConfiguration.getLicenseInfo() } returns createLicenseInfo(tier)

            val model = createLlmModel("test", "Test", "openai", LlmProviderType.CLOUD, isDefault = true)
            every { llmModelSelectionService.getAvailableModels(tier) } returns listOf(model)
            every { llmModelSelectionService.getDefaultModel() } returns model

            val result = handler.handle(query)

            assertNotNull(result)
            verify { llmModelSelectionService.getAvailableModels(tier) }
        }
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

    private fun createLlmModel(
        id: String,
        name: String,
        provider: String,
        providerType: LlmProviderType,
        isDefault: Boolean = false,
        privacyLevel: LlmPrivacyLevel = LlmPrivacyLevel.STANDARD,
        maxTokens: Int = 4096,
        contextWindow: Int = 8192
    ): LlmModel = LlmModel(
        id = id,
        name = name,
        modelId = id,
        description = "Description for $name",
        provider = provider,
        providerType = providerType,
        maxTokens = maxTokens,
        contextWindow = contextWindow,
        supportsStreaming = true,
        supportsFunctionCalling = false,
        privacyLevel = privacyLevel,
        minTier = SubscriptionTier.TRIAL,
        minTierPriority = SubscriptionTier.TRIAL.priority,
        costPer1kInputTokens = BigDecimal("0.01"),
        costPer1kOutputTokens = BigDecimal("0.03"),
        baseUrl = null,
        apiKey = "test-key",
        enabled = true,
        isDefault = isDefault,
        sortOrder = 0,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        capabilities = emptySet()
    )
}
