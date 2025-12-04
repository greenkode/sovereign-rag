package ai.sovereignrag.knowledgebase.configuration.unit

import ai.sovereignrag.knowledgebase.configuration.dto.EmbeddingModelDto
import ai.sovereignrag.knowledgebase.configuration.query.RecommendEmbeddingModelQuery
import ai.sovereignrag.knowledgebase.configuration.query.RecommendEmbeddingModelQueryHandler
import ai.sovereignrag.knowledgebase.configuration.service.WizardConfigurationService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RecommendEmbeddingModelQueryHandlerTest {

    private val wizardConfigurationService: WizardConfigurationService = mockk()

    private lateinit var handler: RecommendEmbeddingModelQueryHandler

    @BeforeEach
    fun setup() {
        handler = RecommendEmbeddingModelQueryHandler(wizardConfigurationService)
    }

    @Test
    fun `should return first enabled model when no languages specified`() {
        val models = listOf(
            createMockEmbeddingModel("model-1", "Model 1"),
            createMockEmbeddingModel("model-2", "Model 2")
        )

        every { wizardConfigurationService.getEnabledEmbeddingModels() } returns models

        val result = handler.handle(RecommendEmbeddingModelQuery(languageCodes = emptySet()))

        assertNotNull(result.recommended)
        assertEquals("model-1", result.recommended!!.id)
        assertEquals(1, result.alternatives.size)
        assertEquals("model-2", result.alternatives[0].id)
        verify { wizardConfigurationService.getEnabledEmbeddingModels() }
    }

    @Test
    fun `should recommend optimized model first when available`() {
        val languageCodes = setOf("en")
        val optimizedModels = listOf(createMockEmbeddingModel("optimized-en", "Optimized EN"))
        val supportedModels = listOf(
            createMockEmbeddingModel("optimized-en", "Optimized EN"),
            createMockEmbeddingModel("multilingual", "Multilingual")
        )

        every { wizardConfigurationService.getOptimizedEmbeddingModels(languageCodes) } returns optimizedModels
        every { wizardConfigurationService.getEmbeddingModelsByLanguages(languageCodes) } returns supportedModels

        val result = handler.handle(RecommendEmbeddingModelQuery(languageCodes = languageCodes))

        assertNotNull(result.recommended)
        assertEquals("optimized-en", result.recommended!!.id)
        assertEquals(1, result.alternatives.size)
        assertEquals("multilingual", result.alternatives[0].id)
    }

    @Test
    fun `should fallback to supported model when no optimized model exists`() {
        val languageCodes = setOf("nl")
        val supportedModels = listOf(createMockEmbeddingModel("multilingual", "Multilingual"))

        every { wizardConfigurationService.getOptimizedEmbeddingModels(languageCodes) } returns emptyList()
        every { wizardConfigurationService.getEmbeddingModelsByLanguages(languageCodes) } returns supportedModels

        val result = handler.handle(RecommendEmbeddingModelQuery(languageCodes = languageCodes))

        assertNotNull(result.recommended)
        assertEquals("multilingual", result.recommended!!.id)
        assertTrue(result.alternatives.isEmpty())
    }

    @Test
    fun `should return null recommended when no models match`() {
        val languageCodes = setOf("xx")

        every { wizardConfigurationService.getOptimizedEmbeddingModels(languageCodes) } returns emptyList()
        every { wizardConfigurationService.getEmbeddingModelsByLanguages(languageCodes) } returns emptyList()

        val result = handler.handle(RecommendEmbeddingModelQuery(languageCodes = languageCodes))

        assertNull(result.recommended)
        assertTrue(result.alternatives.isEmpty())
    }

    @Test
    fun `should exclude recommended model from alternatives`() {
        val languageCodes = setOf("en", "nl")
        val optimizedModels = listOf(createMockEmbeddingModel("model-en", "English Model"))
        val supportedModels = listOf(
            createMockEmbeddingModel("model-en", "English Model"),
            createMockEmbeddingModel("model-nl", "Dutch Model"),
            createMockEmbeddingModel("model-multi", "Multilingual")
        )

        every { wizardConfigurationService.getOptimizedEmbeddingModels(languageCodes) } returns optimizedModels
        every { wizardConfigurationService.getEmbeddingModelsByLanguages(languageCodes) } returns supportedModels

        val result = handler.handle(RecommendEmbeddingModelQuery(languageCodes = languageCodes))

        assertNotNull(result.recommended)
        assertEquals("model-en", result.recommended!!.id)
        assertEquals(2, result.alternatives.size)
        assertTrue(result.alternatives.none { it.id == "model-en" })
    }

    private fun createMockEmbeddingModel(id: String, name: String) = EmbeddingModelDto(
        id = id,
        name = name,
        modelId = id,
        description = "Test model",
        provider = "openai",
        dimensions = 1536,
        maxTokens = 8191,
        supportedLanguages = setOf("en"),
        optimizedFor = setOf("en"),
        enabled = true
    )
}
