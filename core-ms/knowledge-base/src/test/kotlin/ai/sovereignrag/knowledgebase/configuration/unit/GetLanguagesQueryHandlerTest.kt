package ai.sovereignrag.knowledgebase.configuration.unit

import ai.sovereignrag.knowledgebase.configuration.dto.LanguageDto
import ai.sovereignrag.knowledgebase.configuration.query.GetLanguagesQuery
import ai.sovereignrag.knowledgebase.configuration.query.GetLanguagesQueryHandler
import ai.sovereignrag.knowledgebase.configuration.service.WizardConfigurationService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetLanguagesQueryHandlerTest {

    private val wizardConfigurationService: WizardConfigurationService = mockk()

    private lateinit var handler: GetLanguagesQueryHandler

    @BeforeEach
    fun setup() {
        handler = GetLanguagesQueryHandler(wizardConfigurationService)
    }

    @Test
    fun `should return enabled languages`() {
        val languages = listOf(
            LanguageDto("en", "English", "English", true),
            LanguageDto("nl", "Dutch", "Nederlands", true),
            LanguageDto("de", "German", "Deutsch", true)
        )

        every { wizardConfigurationService.getEnabledLanguages() } returns languages

        val result = handler.handle(GetLanguagesQuery())

        assertEquals(3, result.languages.size)
        assertEquals("en", result.languages[0].code)
        assertEquals("English", result.languages[0].name)
        assertEquals("Nederlands", result.languages[1].nativeName)
        verify { wizardConfigurationService.getEnabledLanguages() }
    }

    @Test
    fun `should return empty list when no languages exist`() {
        every { wizardConfigurationService.getEnabledLanguages() } returns emptyList()

        val result = handler.handle(GetLanguagesQuery())

        assertTrue(result.languages.isEmpty())
    }
}
