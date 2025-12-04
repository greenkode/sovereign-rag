package ai.sovereignrag.knowledgebase.configuration.unit

import ai.sovereignrag.knowledgebase.configuration.dto.RegionDto
import ai.sovereignrag.knowledgebase.configuration.query.GetRegionsQuery
import ai.sovereignrag.knowledgebase.configuration.query.GetRegionsQueryHandler
import ai.sovereignrag.knowledgebase.configuration.service.WizardConfigurationService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetRegionsQueryHandlerTest {

    private val wizardConfigurationService: WizardConfigurationService = mockk()

    private lateinit var handler: GetRegionsQueryHandler

    @BeforeEach
    fun setup() {
        handler = GetRegionsQueryHandler(wizardConfigurationService)
    }

    @Test
    fun `should return enabled regions`() {
        val regions = listOf(
            RegionDto("eu-west-1", "EU West", "Europe", "Frankfurt", "Germany", "DE", "🇩🇪", true),
            RegionDto("us-east-1", "US East", "North America", "Virginia", "United States", "US", "🇺🇸", true)
        )

        every { wizardConfigurationService.getEnabledRegions() } returns regions

        val result = handler.handle(GetRegionsQuery())

        assertEquals(2, result.regions.size)
        assertEquals("eu-west-1", result.regions[0].code)
        assertEquals("Europe", result.regions[0].continent)
        verify { wizardConfigurationService.getEnabledRegions() }
    }

    @Test
    fun `should return empty list when no regions exist`() {
        every { wizardConfigurationService.getEnabledRegions() } returns emptyList()

        val result = handler.handle(GetRegionsQuery())

        assertTrue(result.regions.isEmpty())
    }
}
