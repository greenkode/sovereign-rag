package ai.sovereignrag.knowledgebase.configuration.unit

import ai.sovereignrag.knowledgebase.configuration.dto.EmbeddingModelDto
import ai.sovereignrag.knowledgebase.configuration.dto.LanguageDto
import ai.sovereignrag.knowledgebase.configuration.dto.RegionDto
import ai.sovereignrag.knowledgebase.configuration.query.GetWizardConfigurationQuery
import ai.sovereignrag.knowledgebase.configuration.query.GetWizardConfigurationQueryHandler
import ai.sovereignrag.knowledgebase.configuration.service.WizardConfigurationService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class GetWizardConfigurationQueryHandlerTest {

    private val wizardConfigurationService: WizardConfigurationService = mockk()

    private lateinit var handler: GetWizardConfigurationQueryHandler

    @BeforeEach
    fun setup() {
        handler = GetWizardConfigurationQueryHandler(wizardConfigurationService)
    }

    @Test
    fun `should return all enabled configuration data`() {
        val regions = listOf(
            RegionDto("eu-west-1", "EU West", "Europe", "Frankfurt", "Germany", "DE", "🇩🇪", true),
            RegionDto("us-east-1", "US East", "North America", "Virginia", "United States", "US", "🇺🇸", true)
        )
        val languages = listOf(
            LanguageDto("en", "English", "English", true),
            LanguageDto("nl", "Dutch", "Nederlands", true)
        )
        val embeddingModels = listOf(
            EmbeddingModelDto("openai-text-3-small", "OpenAI Text Small", "text-embedding-3-small", "Small embedding model", "openai", 1536, 8191, setOf("en"), setOf("en"), true)
        )

        every { wizardConfigurationService.getEnabledRegions() } returns regions
        every { wizardConfigurationService.getEnabledLanguages() } returns languages
        every { wizardConfigurationService.getEnabledEmbeddingModels() } returns embeddingModels

        val result = handler.handle(GetWizardConfigurationQuery())

        assertEquals(2, result.regions.size)
        assertEquals(2, result.languages.size)
        assertEquals(1, result.embeddingModels.size)
        assertEquals("eu-west-1", result.regions[0].code)
        assertEquals("en", result.languages[0].code)
        assertEquals("openai-text-3-small", result.embeddingModels[0].id)

        verify { wizardConfigurationService.getEnabledRegions() }
        verify { wizardConfigurationService.getEnabledLanguages() }
        verify { wizardConfigurationService.getEnabledEmbeddingModels() }
    }

    @Test
    fun `should return empty lists when no configuration data exists`() {
        every { wizardConfigurationService.getEnabledRegions() } returns emptyList()
        every { wizardConfigurationService.getEnabledLanguages() } returns emptyList()
        every { wizardConfigurationService.getEnabledEmbeddingModels() } returns emptyList()

        val result = handler.handle(GetWizardConfigurationQuery())

        assertEquals(0, result.regions.size)
        assertEquals(0, result.languages.size)
        assertEquals(0, result.embeddingModels.size)
    }
}
