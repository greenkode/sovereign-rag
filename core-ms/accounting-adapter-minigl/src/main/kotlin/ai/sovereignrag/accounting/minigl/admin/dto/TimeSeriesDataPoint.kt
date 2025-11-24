package ai.sovereignrag.accounting.minigl.admin.dto

data class TimeSeriesDataPoint(
    val month: String,
    val assets: Double,
    val liabilities: Double,
    val equity: Double,
    val revenue: Double,
    val expenses: Double
)