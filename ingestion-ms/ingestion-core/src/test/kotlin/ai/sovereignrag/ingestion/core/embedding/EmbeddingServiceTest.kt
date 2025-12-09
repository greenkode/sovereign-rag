package ai.sovereignrag.ingestion.core.embedding

import ai.sovereignrag.commons.embedding.EmbeddingModelConfig
import ai.sovereignrag.ingestion.commons.config.EmbeddingProperties
import ai.sovereignrag.ingestion.commons.config.IngestionProperties
import ai.sovereignrag.ingestion.commons.config.PgVectorProperties
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class EmbeddingServiceTest {

    private lateinit var ingestionProperties: IngestionProperties
    private lateinit var service: EmbeddingService

    @BeforeEach
    fun setup() {
        ingestionProperties = IngestionProperties(
            embedding = EmbeddingProperties(
                modelName = "test-model",
                ollamaBaseUrl = "http://localhost:11434",
                openaiApiKey = "test-openai-key",
                huggingfaceApiKey = "test-hf-key",
                cohereApiKey = "test-cohere-key",
                batchSize = 10,
                dimension = 768,
                pgvector = PgVectorProperties()
            )
        )
        service = EmbeddingService(ingestionProperties)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `getModel should return null for uncached model`() {
        val config = createMockModelConfig("new-model", "openai")

        val getModelMethod = EmbeddingService::class.java.getDeclaredMethod("getModel", EmbeddingModelConfig::class.java)
        getModelMethod.isAccessible = true

        val result = getModelMethod.invoke(service, config)

        assertEquals(null, result)
    }

    @Test
    fun `createOpenAiModel should throw when API key not configured`() {
        val propertiesWithoutKey = IngestionProperties(
            embedding = EmbeddingProperties(
                openaiApiKey = null
            )
        )
        val serviceWithoutKey = EmbeddingService(propertiesWithoutKey)
        val config = createMockModelConfig("openai-model", "openai")

        val createModelMethod = EmbeddingService::class.java.getDeclaredMethod("createOpenAiModel", EmbeddingModelConfig::class.java)
        createModelMethod.isAccessible = true

        assertThrows<IllegalStateException> {
            try {
                createModelMethod.invoke(serviceWithoutKey, config)
            } catch (e: java.lang.reflect.InvocationTargetException) {
                throw e.cause!!
            }
        }
    }

    @Test
    fun `createHuggingFaceModel should throw when API key not configured`() {
        val propertiesWithoutKey = IngestionProperties(
            embedding = EmbeddingProperties(
                huggingfaceApiKey = null
            )
        )
        val serviceWithoutKey = EmbeddingService(propertiesWithoutKey)
        val config = createMockModelConfig("hf-model", "huggingface")

        val createModelMethod = EmbeddingService::class.java.getDeclaredMethod("createHuggingFaceModel", EmbeddingModelConfig::class.java)
        createModelMethod.isAccessible = true

        assertThrows<IllegalStateException> {
            try {
                createModelMethod.invoke(serviceWithoutKey, config)
            } catch (e: java.lang.reflect.InvocationTargetException) {
                throw e.cause!!
            }
        }
    }

    @Test
    fun `createModel should use baseUrl from config for Ollama`() {
        val config = createMockModelConfig("ollama-model", "ollama", baseUrl = "http://custom-ollama:11434")

        val createModelMethod = EmbeddingService::class.java.getDeclaredMethod("createOllamaModel", EmbeddingModelConfig::class.java)
        createModelMethod.isAccessible = true

        val model = createModelMethod.invoke(service, config)

        assertNotNull(model)
    }

    @Test
    fun `createModel should use default Ollama URL when baseUrl not in config`() {
        val config = createMockModelConfig("ollama-model", "ollama", baseUrl = null)

        val createModelMethod = EmbeddingService::class.java.getDeclaredMethod("createOllamaModel", EmbeddingModelConfig::class.java)
        createModelMethod.isAccessible = true

        val model = createModelMethod.invoke(service, config)

        assertNotNull(model)
    }

    @Test
    fun `createModel should fallback to Ollama for unknown provider`() {
        val config = createMockModelConfig("unknown-model", "unknown-provider")

        val createModelMethod = EmbeddingService::class.java.getDeclaredMethod("createModel", EmbeddingModelConfig::class.java)
        createModelMethod.isAccessible = true

        val model = createModelMethod.invoke(service, config)

        assertNotNull(model)
    }

    private fun createMockModelConfig(
        id: String,
        provider: String,
        modelId: String = id,
        dimensions: Int = 1536,
        maxTokens: Int = 8191,
        baseUrl: String? = null
    ): EmbeddingModelConfig {
        return object : EmbeddingModelConfig {
            override val id: String = id
            override val name: String = "Test Model $id"
            override val modelId: String = modelId
            override val provider: String = provider
            override val dimensions: Int = dimensions
            override val maxTokens: Int = maxTokens
            override val baseUrl: String? = baseUrl
        }
    }
}
