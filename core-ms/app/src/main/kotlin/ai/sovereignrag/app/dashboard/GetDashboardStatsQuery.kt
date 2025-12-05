package ai.sovereignrag.app.dashboard

import an.awesome.pipelinr.Command

class GetDashboardStatsQuery : Command<GetDashboardStatsResult>

data class GetDashboardStatsResult(
    val knowledgeBasesCount: Int,
    val aiAgentsCount: Int,
    val apiRequestsCount: Long,
    val knowledgeSourcesCount: Long
)
