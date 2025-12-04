package ai.sovereignrag.knowledgebase.configuration.unit

import ai.sovereignrag.knowledgebase.configuration.dto.EmbeddingModelDto
import ai.sovereignrag.knowledgebase.configuration.query.GetEmbeddingModelsQuery
import ai.sovereignrag.knowledgebase.configuration.query.GetEmbeddingModelsQueryHandler
import ai.sovereignrag.knowledgebase.configuration.service.WizardConfigurationService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetEmbeddingModelsQueryHandlerTest {

    private val wizardConfigurationService: WizardConfigurationService = mockk()

    private lateinit var handler: GetEmbeddingModelsQueryHandler

    @BeforeEach
    fun setup() {
        handler = GetEmbeddingModelsQueryHandler(wizardConfigurationService)
    }

    @Test
    fun `should return all enabled embedding models when no language filter`() {
        val models = listOf(
            createMockEmbeddingModel("openai-text-3-small", "OpenAI Text Small", setOf("en"), setOf("en")),
            createMockEmbeddingModel("multilingual-e5-large", "Multilingual E5 Large", setOf("en", "nl", "de"), setOf())
        )

        every { wizardConfigurationService.getEnabledEmbeddingModels() } returns models

        val result = handler.handle(GetEmbeddingModelsQuery())

        assertEquals(2, result.embeddingModels.size)
        verify { wizardConfigurationService.getEnabledEmbeddingModels() }
    }

    @Test
    fun `should return all enabled models when language filter is empty`() {
        val models = listOf(
            createMockEmbeddingModel("openai-text-3-small", "OpenAI Text Small", setOf("en"), setOf("en"))
        )

        every { wizardConfigurationService.getEnabledEmbeddingModels() } returns models

        val result = handler.handle(GetEmbeddingModelsQuery(languageCodes = emptySet()))

        assertEquals(1, result.embeddingModels.size)
        verify { wizardConfigurationService.getEnabledEmbeddingModels() }
    }

    @Test
    fun `should filter by supported languages when language codes provided`() {
        val filteredModels = listOf(
            createMockEmbeddingModel("multilingual-e5-large", "Multilingual E5 Large", setOf("en", "nl", "de"), setOf())
        )
        val languageCodes = setOf("nl", "de")

        every { wizardConfigurationService.getEmbeddingModelsByLanguages(languageCodes) } returns filteredModels

        val result = handler.handle(GetEmbeddingModelsQuery(languageCodes = languageCodes))

        assertEquals(1, result.embeddingModels.size)
        assertEquals("multilingual-e5-large", result.embeddingModels[0].id)
        verify { wizardConfigurationService.getEmbeddingModelsByLanguages(languageCodes) }
    }

    @Test
    fun `should return empty list when no models match languages`() {
        val languageCodes = setOf("zh")

        every { wizardConfigurationService.getEmbeddingModelsByLanguages(languageCodes) } returns emptyList()

        val result = handler.handle(GetEmbeddingModelsQuery(languageCodes = languageCodes))

        assertTrue(result.embeddingModels.isEmpty())
    }

    private fun createMockEmbeddingModel(
        id: String,
        name: String,
        supportedLanguages: Set<String>,
        optimizedFor: Set<String>
    ) = EmbeddingModelDto(
        id = id,
        name = name,
        modelId = id,
        description = "Test model",
        provider = "openai",
        dimensions = 1536,
        maxTokens = 8191,
        supportedLanguages = supportedLanguages,
        optimizedFor = optimizedFor,
        enabled = true
    )
}
