package ai.sovereignrag.app.unit

import ai.sovereignrag.app.dashboard.GetDashboardStatsQuery
import ai.sovereignrag.app.dashboard.GetDashboardStatsQueryHandler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GetDashboardStatsQueryHandlerTest {

    private lateinit var handler: GetDashboardStatsQueryHandler

    @BeforeEach
    fun setup() {
        handler = GetDashboardStatsQueryHandler()
    }

    @Test
    fun `should return dashboard stats with zero counts`() {
        val query = GetDashboardStatsQuery()

        val result = handler.handle(query)

        assertEquals(0, result.knowledgeBasesCount)
        assertEquals(0, result.aiAgentsCount)
        assertEquals(0L, result.apiRequestsCount)
        assertEquals(0L, result.knowledgeSourcesCount)
    }
}
