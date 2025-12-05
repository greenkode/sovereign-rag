package ai.sovereignrag.app.dashboard

data class DashboardStatsResponse(
    val knowledgeBasesCount: Int,
    val aiAgentsCount: Int,
    val apiRequestsCount: Long,
    val knowledgeSourcesCount: Long
)
