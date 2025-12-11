package ai.sovereignrag.core.llm

import ai.sovereignrag.commons.subscription.SubscriptionTier
import ai.sovereignrag.knowledgebase.configuration.domain.LlmModel
import ai.sovereignrag.knowledgebase.configuration.domain.LlmPrivacyLevel
import ai.sovereignrag.knowledgebase.configuration.domain.LlmProviderType
import ai.sovereignrag.knowledgebase.configuration.repository.LlmModelRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.Instant
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LlmModelSelectionServiceTest {

    private val llmModelRepository: LlmModelRepository = mockk()
    private lateinit var service: LlmModelSelectionService

    @BeforeEach
    fun setup() {
        service = LlmModelSelectionService(llmModelRepository)
    }

    @Test
    fun `should get model by ID`() {
        val modelId = "gpt-4"
        val model = createLlmModel(modelId, "GPT-4", "openai", LlmProviderType.CLOUD)

        every { llmModelRepository.findById(modelId) } returns Optional.of(model)

        val result = service.getModelById(modelId)

        assertNotNull(result)
        assertEquals(modelId, result.id)
    }

    @Test
    fun `should return null for non-existent model ID`() {
        every { llmModelRepository.findById("non-existent") } returns Optional.empty()

        val result = service.getModelById("non-existent")

        assertNull(result)
    }

    @Test
    fun `should get default model`() {
        val defaultModel = createLlmModel("gpt-4", "GPT-4", "openai", LlmProviderType.CLOUD, isDefault = true)

        every { llmModelRepository.findByIsDefaultTrueAndEnabledTrue() } returns defaultModel

        val result = service.getDefaultModel()

        assertEquals("gpt-4", result.id)
        assertTrue(result.isDefault)
    }

    @Test
    fun `should fallback to local model when no default model configured`() {
        val localModel = createLlmModel("llama2", "Llama 2", "ollama", LlmProviderType.LOCAL)

        every { llmModelRepository.findByIsDefaultTrueAndEnabledTrue() } returns null
        every { llmModelRepository.findLocalModels() } returns listOf(localModel)

        val result = service.getDefaultModel()

        assertEquals("llama2", result.id)
    }

    @Test
    fun `should throw exception when no models configured`() {
        every { llmModelRepository.findByIsDefaultTrueAndEnabledTrue() } returns null
        every { llmModelRepository.findLocalModels() } returns emptyList()

        assertThrows<IllegalStateException> {
            service.getDefaultModel()
        }
    }

    @Test
    fun `should get available models for tier`() {
        val tier = SubscriptionTier.PROFESSIONAL
        val models = listOf(
            createLlmModel("gpt-4", "GPT-4", "openai", LlmProviderType.CLOUD),
            createLlmModel("claude-3", "Claude 3", "anthropic", LlmProviderType.CLOUD)
        )

        every { llmModelRepository.findAccessibleByTier(tier.priority) } returns models

        val result = service.getAvailableModels(tier)

        assertEquals(2, result.size)
        verify { llmModelRepository.findAccessibleByTier(tier.priority) }
    }

    @Test
    fun `should get privacy compliant models for tier`() {
        val tier = SubscriptionTier.ENTERPRISE
        val localModels = listOf(
            createLlmModel("llama2", "Llama 2", "ollama", LlmProviderType.LOCAL, privacyLevel = LlmPrivacyLevel.MAXIMUM)
        )

        every { llmModelRepository.findByPrivacyLevelAndTier(LlmPrivacyLevel.MAXIMUM, tier.priority) } returns localModels

        val result = service.getPrivacyCompliantModels(tier)

        assertEquals(1, result.size)
        assertEquals("llama2", result.first().id)
    }

    @Test
    fun `should get cloud models for tier`() {
        val tier = SubscriptionTier.PROFESSIONAL
        val cloudModels = listOf(
            createLlmModel("gpt-4", "GPT-4", "openai", LlmProviderType.CLOUD)
        )

        every { llmModelRepository.findCloudModelsAccessibleByTier(tier.priority) } returns cloudModels

        val result = service.getCloudModels(tier)

        assertEquals(1, result.size)
        assertEquals(LlmProviderType.CLOUD, result.first().providerType)
    }

    @Test
    fun `should validate model access - allowed`() {
        val modelId = "gpt-4"
        val model = createLlmModel(modelId, "GPT-4", "openai", LlmProviderType.CLOUD, minTier = SubscriptionTier.TRIAL)

        every { llmModelRepository.findById(modelId) } returns Optional.of(model)

        val result = service.validateModelAccess(modelId, SubscriptionTier.PROFESSIONAL)

        assertTrue(result is ModelAccessResult.Allowed)
        assertEquals(model, (result as ModelAccessResult.Allowed).model)
    }

    @Test
    fun `should validate model access - not found`() {
        every { llmModelRepository.findById("non-existent") } returns Optional.empty()

        val result = service.validateModelAccess("non-existent", SubscriptionTier.PROFESSIONAL)

        assertTrue(result is ModelAccessResult.NotFound)
    }

    @Test
    fun `should validate model access - disabled`() {
        val model = createLlmModel("disabled-model", "Disabled", "openai", LlmProviderType.CLOUD, enabled = false)

        every { llmModelRepository.findById("disabled-model") } returns Optional.of(model)

        val result = service.validateModelAccess("disabled-model", SubscriptionTier.PROFESSIONAL)

        assertTrue(result is ModelAccessResult.Disabled)
    }

    @Test
    fun `should validate model access - tier restricted`() {
        val model = createLlmModel(
            "enterprise-model",
            "Enterprise Model",
            "openai",
            LlmProviderType.CLOUD,
            minTier = SubscriptionTier.ENTERPRISE
        )

        every { llmModelRepository.findById("enterprise-model") } returns Optional.of(model)

        val result = service.validateModelAccess("enterprise-model", SubscriptionTier.TRIAL)

        assertTrue(result is ModelAccessResult.TierRestricted)
        assertEquals(SubscriptionTier.ENTERPRISE, (result as ModelAccessResult.TierRestricted).requiredTier)
    }

    @Test
    fun `should resolve model for knowledge base - specific model allowed`() {
        val model = createLlmModel("gpt-4", "GPT-4", "openai", LlmProviderType.CLOUD)

        every { llmModelRepository.findById("gpt-4") } returns Optional.of(model)

        val result = service.resolveModelForKnowledgeBase("gpt-4", SubscriptionTier.PROFESSIONAL)

        assertEquals("gpt-4", result.id)
    }

    @Test
    fun `should resolve model for knowledge base - fallback when tier restricted`() {
        val enterpriseModel = createLlmModel(
            "enterprise-only",
            "Enterprise Only",
            "openai",
            LlmProviderType.CLOUD,
            minTier = SubscriptionTier.ENTERPRISE
        )
        val defaultModel = createLlmModel(
            "gpt-3.5",
            "GPT-3.5",
            "openai",
            LlmProviderType.CLOUD,
            isDefault = true,
            minTier = SubscriptionTier.TRIAL
        )

        every { llmModelRepository.findById("enterprise-only") } returns Optional.of(enterpriseModel)
        every { llmModelRepository.findByIsDefaultTrueAndEnabledTrue() } returns defaultModel

        val result = service.resolveModelForKnowledgeBase("enterprise-only", SubscriptionTier.TRIAL)

        assertEquals("gpt-3.5", result.id)
    }

    @Test
    fun `should resolve model for knowledge base - default when no model ID provided`() {
        val defaultModel = createLlmModel("gpt-4", "GPT-4", "openai", LlmProviderType.CLOUD, isDefault = true)

        every { llmModelRepository.findByIsDefaultTrueAndEnabledTrue() } returns defaultModel

        val result = service.resolveModelForKnowledgeBase(null, SubscriptionTier.PROFESSIONAL)

        assertEquals("gpt-4", result.id)
    }

    @Test
    fun `should resolve model for knowledge base - privacy required`() {
        val localModel = createLlmModel(
            "llama2",
            "Llama 2",
            "ollama",
            LlmProviderType.LOCAL,
            privacyLevel = LlmPrivacyLevel.MAXIMUM
        )

        every { llmModelRepository.findByPrivacyLevelAndTier(LlmPrivacyLevel.MAXIMUM, SubscriptionTier.PROFESSIONAL.priority) } returns listOf(localModel)

        val result = service.resolveModelForKnowledgeBase(null, SubscriptionTier.PROFESSIONAL, requiresPrivacy = true)

        assertEquals("llama2", result.id)
    }

    @Test
    fun `should get models by capabilities`() {
        val tier = SubscriptionTier.PROFESSIONAL
        val capabilities = setOf("function_calling", "vision")
        val models = listOf(
            createLlmModel("gpt-4-vision", "GPT-4 Vision", "openai", LlmProviderType.CLOUD, capabilities = capabilities)
        )

        every { llmModelRepository.findByCapabilitiesAndTier(capabilities, tier.priority) } returns models

        val result = service.getModelsByCapability(capabilities, tier)

        assertEquals(1, result.size)
    }

    @Test
    fun `should get models by provider`() {
        val models = listOf(
            createLlmModel("gpt-4", "GPT-4", "openai", LlmProviderType.CLOUD),
            createLlmModel("gpt-3.5", "GPT-3.5", "openai", LlmProviderType.CLOUD),
            createLlmModel("claude", "Claude", "anthropic", LlmProviderType.CLOUD)
        )

        every { llmModelRepository.findByEnabledTrueOrderBySortOrder() } returns models

        val result = service.getModelsByProvider("openai")

        assertEquals(2, result.size)
        assertTrue(result.all { it.provider.equals("openai", ignoreCase = true) })
    }

    @Test
    fun `should get local models`() {
        val localModels = listOf(
            createLlmModel("llama2", "Llama 2", "ollama", LlmProviderType.LOCAL),
            createLlmModel("mistral", "Mistral", "ollama", LlmProviderType.LOCAL)
        )

        every { llmModelRepository.findByProviderTypeAndEnabledTrueOrderBySortOrder(LlmProviderType.LOCAL) } returns localModels

        val result = service.getLocalModels()

        assertEquals(2, result.size)
        assertTrue(result.all { it.providerType == LlmProviderType.LOCAL })
    }

    @Test
    fun `should get all enabled models`() {
        val models = listOf(
            createLlmModel("gpt-4", "GPT-4", "openai", LlmProviderType.CLOUD),
            createLlmModel("llama2", "Llama 2", "ollama", LlmProviderType.LOCAL)
        )

        every { llmModelRepository.findByEnabledTrueOrderBySortOrder() } returns models

        val result = service.getAllEnabledModels()

        assertEquals(2, result.size)
    }

    private fun createLlmModel(
        id: String,
        name: String,
        provider: String,
        providerType: LlmProviderType,
        isDefault: Boolean = false,
        enabled: Boolean = true,
        privacyLevel: LlmPrivacyLevel = LlmPrivacyLevel.STANDARD,
        minTier: SubscriptionTier = SubscriptionTier.TRIAL,
        capabilities: Set<String> = emptySet()
    ): LlmModel = LlmModel(
        id = id,
        name = name,
        modelId = id,
        description = "Description for $name",
        provider = provider,
        providerType = providerType,
        maxTokens = 4096,
        contextWindow = 8192,
        supportsStreaming = true,
        supportsFunctionCalling = false,
        privacyLevel = privacyLevel,
        minTier = minTier,
        minTierPriority = minTier.priority,
        costPer1kInputTokens = BigDecimal("0.01"),
        costPer1kOutputTokens = BigDecimal("0.03"),
        baseUrl = null,
        apiKey = "test-key",
        enabled = enabled,
        isDefault = isDefault,
        sortOrder = 0,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        capabilities = capabilities
    )
}
