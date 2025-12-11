package ai.sovereignrag.core.llm

import ai.sovereignrag.commons.subscription.SubscriptionTier
import ai.sovereignrag.knowledgebase.configuration.domain.LlmModel
import ai.sovereignrag.knowledgebase.configuration.domain.LlmPrivacyLevel
import ai.sovereignrag.knowledgebase.configuration.domain.LlmProviderType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LlmModelFactoryTest {

    private lateinit var factory: LlmModelFactory

    @BeforeEach
    fun setup() {
        factory = LlmModelFactory()
    }

    @Test
    fun `should create OpenAI compatible chat model`() {
        val model = createLlmModel(
            id = "openai-gpt4",
            provider = "openai",
            providerType = LlmProviderType.CLOUD,
            apiKey = "test-api-key",
            modelId = "gpt-4"
        )

        val chatModel = factory.createChatModel(model)

        assertNotNull(chatModel)
    }

    @Test
    fun `should create OpenAI compatible streaming model`() {
        val model = createLlmModel(
            id = "openai-gpt4-stream",
            provider = "openai",
            providerType = LlmProviderType.CLOUD,
            apiKey = "test-api-key",
            modelId = "gpt-4"
        )

        val streamingModel = factory.createStreamingChatModel(model)

        assertNotNull(streamingModel)
    }

    @Test
    fun `should throw exception for OpenAI model without API key`() {
        val model = createLlmModel(
            id = "openai-no-key",
            provider = "openai",
            providerType = LlmProviderType.CLOUD,
            apiKey = null,
            modelId = "gpt-4"
        )

        assertThrows<IllegalStateException> {
            factory.createChatModel(model)
        }
    }

    @Test
    fun `should create Anthropic chat model`() {
        val model = createLlmModel(
            id = "claude-model",
            provider = "anthropic",
            providerType = LlmProviderType.CLOUD,
            apiKey = "test-anthropic-key",
            modelId = "claude-3-opus"
        )

        val chatModel = factory.createChatModel(model)

        assertNotNull(chatModel)
    }

    @Test
    fun `should create Anthropic streaming model`() {
        val model = createLlmModel(
            id = "claude-stream",
            provider = "claude",
            providerType = LlmProviderType.CLOUD,
            apiKey = "test-anthropic-key",
            modelId = "claude-3-opus"
        )

        val streamingModel = factory.createStreamingChatModel(model)

        assertNotNull(streamingModel)
    }

    @Test
    fun `should create Ollama chat model with base URL`() {
        val model = createLlmModel(
            id = "ollama-local",
            provider = "ollama",
            providerType = LlmProviderType.LOCAL,
            baseUrl = "http://localhost:11434",
            modelId = "llama2"
        )

        val chatModel = factory.createChatModel(model)

        assertNotNull(chatModel)
    }

    @Test
    fun `should create Ollama streaming model`() {
        val model = createLlmModel(
            id = "ollama-stream",
            provider = "ollama",
            providerType = LlmProviderType.LOCAL,
            baseUrl = "http://localhost:11434",
            modelId = "llama2"
        )

        val streamingModel = factory.createStreamingChatModel(model)

        assertNotNull(streamingModel)
    }

    @Test
    fun `should throw exception for Ollama model without base URL`() {
        val model = createLlmModel(
            id = "ollama-no-url",
            provider = "ollama",
            providerType = LlmProviderType.LOCAL,
            baseUrl = null,
            modelId = "llama2"
        )

        assertThrows<IllegalStateException> {
            factory.createChatModel(model)
        }
    }

    @Test
    fun `should create Ollama Cloud model with API key in custom headers`() {
        val model = createLlmModel(
            id = "ollama-cloud",
            provider = "ollama-cloud",
            providerType = LlmProviderType.CLOUD,
            baseUrl = "https://ollama-cloud.example.com",
            apiKey = "bearer-token-123",
            modelId = "llama2"
        )

        val chatModel = factory.createChatModel(model)

        assertNotNull(chatModel)
    }

    @Test
    fun `should throw exception for unsupported provider`() {
        val model = createLlmModel(
            id = "unknown-provider",
            provider = "unknown-provider",
            providerType = LlmProviderType.CLOUD,
            apiKey = "test-key",
            modelId = "test-model"
        )

        assertThrows<IllegalArgumentException> {
            factory.createChatModel(model)
        }
    }

    @Test
    fun `should cache chat models`() {
        val model = createLlmModel(
            id = "cached-model",
            provider = "openai",
            providerType = LlmProviderType.CLOUD,
            apiKey = "test-api-key",
            modelId = "gpt-4"
        )

        val chatModel1 = factory.createChatModel(model)
        val chatModel2 = factory.createChatModel(model)

        assertTrue(chatModel1 === chatModel2)
    }

    @Test
    fun `should cache streaming models`() {
        val model = createLlmModel(
            id = "cached-stream-model",
            provider = "openai",
            providerType = LlmProviderType.CLOUD,
            apiKey = "test-api-key",
            modelId = "gpt-4"
        )

        val streamingModel1 = factory.createStreamingChatModel(model)
        val streamingModel2 = factory.createStreamingChatModel(model)

        assertTrue(streamingModel1 === streamingModel2)
    }

    @Test
    fun `should clear cache`() {
        val model = createLlmModel(
            id = "clearable-model",
            provider = "openai",
            providerType = LlmProviderType.CLOUD,
            apiKey = "test-api-key",
            modelId = "gpt-4"
        )

        val chatModel1 = factory.createChatModel(model)
        factory.clearCache()
        val chatModel2 = factory.createChatModel(model)

        assertTrue(chatModel1 !== chatModel2)
    }

    @Test
    fun `should evict specific model from cache`() {
        val model = createLlmModel(
            id = "evictable-model",
            provider = "openai",
            providerType = LlmProviderType.CLOUD,
            apiKey = "test-api-key",
            modelId = "gpt-4"
        )

        val chatModel1 = factory.createChatModel(model)
        factory.evictFromCache("evictable-model")
        val chatModel2 = factory.createChatModel(model)

        assertTrue(chatModel1 !== chatModel2)
    }

    @Test
    fun `should support OpenAI compatible providers`() {
        val compatibleProviders = listOf("azure", "azure-openai", "together", "anyscale", "fireworks", "groq", "deepseek")

        compatibleProviders.forEach { provider ->
            val model = createLlmModel(
                id = "$provider-model",
                provider = provider,
                providerType = LlmProviderType.CLOUD,
                apiKey = "test-key",
                modelId = "test-model",
                baseUrl = "https://api.example.com"
            )

            val chatModel = factory.createChatModel(model)
            assertNotNull(chatModel, "Should create model for provider: $provider")
        }
    }

    private fun createLlmModel(
        id: String,
        provider: String,
        providerType: LlmProviderType,
        modelId: String,
        apiKey: String? = null,
        baseUrl: String? = null
    ): LlmModel = LlmModel(
        id = id,
        name = "Test Model $id",
        modelId = modelId,
        description = "Test description",
        provider = provider,
        providerType = providerType,
        maxTokens = 4096,
        contextWindow = 8192,
        supportsStreaming = true,
        supportsFunctionCalling = false,
        privacyLevel = if (providerType == LlmProviderType.LOCAL) LlmPrivacyLevel.MAXIMUM else LlmPrivacyLevel.STANDARD,
        minTier = SubscriptionTier.TRIAL,
        minTierPriority = SubscriptionTier.TRIAL.priority,
        costPer1kInputTokens = BigDecimal("0.01"),
        costPer1kOutputTokens = BigDecimal("0.03"),
        baseUrl = baseUrl,
        apiKey = apiKey,
        enabled = true,
        isDefault = false,
        sortOrder = 0,
        createdAt = Instant.now(),
        updatedAt = Instant.now(),
        capabilities = emptySet()
    )
}
