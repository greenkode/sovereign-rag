package ai.sovereignrag.app.dashboard

import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class GetDashboardStatsQueryHandler : Command.Handler<GetDashboardStatsQuery, GetDashboardStatsResult> {

    override fun handle(query: GetDashboardStatsQuery): GetDashboardStatsResult {
        log.info { "Processing GetDashboardStatsQuery" }

        return GetDashboardStatsResult(
            knowledgeBasesCount = 0,
            aiAgentsCount = 0,
            apiRequestsCount = 0L,
            documentsCount = 0L
        )
    }
}
