package ai.sovereignrag.app.dashboard

import an.awesome.pipelinr.Pipeline
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/dashboard")
class DashboardController(
    private val pipeline: Pipeline
) {

    @GetMapping("/stats")
    fun getDashboardStats(): DashboardStatsResponse {
        log.info { "Getting dashboard statistics" }

        val result = pipeline.send(GetDashboardStatsQuery())

        return DashboardStatsResponse(
            knowledgeBasesCount = result.knowledgeBasesCount,
            aiAgentsCount = result.aiAgentsCount,
            apiRequestsCount = result.apiRequestsCount,
            documentsCount = result.documentsCount
        )
    }
}
