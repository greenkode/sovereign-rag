package ai.sovereignrag.accounting.minigl.admin.service

import ai.sovereignrag.accounting.minigl.admin.dto.TimeSeriesDataPoint


class FinancialDataService {
    
    private val timeSeriesData: List<TimeSeriesDataPoint> = listOf(
        TimeSeriesDataPoint(
            month = "Jan",
            assets = 120000.0,
            liabilities = 42000.0,
            equity = 78000.0,
            revenue = 15000.0,
            expenses = 10000.0
        ),
        TimeSeriesDataPoint(
            month = "Feb",
            assets = 118000.0,
            liabilities = 41000.0,
            equity = 77000.0,
            revenue = 16000.0,
            expenses = 11000.0
        ),
        TimeSeriesDataPoint(
            month = "Mar",
            assets = 122000.0,
            liabilities = 43000.0,
            equity = 79000.0,
            revenue = 18000.0,
            expenses = 12000.0
        ),
        TimeSeriesDataPoint(
            month = "Apr",
            assets = 124000.0,
            liabilities = 44000.0,
            equity = 80000.0,
            revenue = 17000.0,
            expenses = 9000.0
        ),
        TimeSeriesDataPoint(
            month = "May",
            assets = 125000.0,
            liabilities = 45000.0,
            equity = 80000.0,
            revenue = 19000.0,
            expenses = 11000.0
        ),
        TimeSeriesDataPoint(
            month = "Jun",
            assets = 125000.0,
            liabilities = 45000.0,
            equity = 80000.0,
            revenue = 20000.0,
            expenses = 12000.0
        )
    )

    fun getTimeSeriesData(): List<TimeSeriesDataPoint> = timeSeriesData

    fun getSummaryMetrics(): Map<String, Any> {
        val latest = timeSeriesData.last()
        val totalRevenue = timeSeriesData.sumOf { it.revenue }
        val totalExpenses = timeSeriesData.sumOf { it.expenses }
        val netIncome = totalRevenue - totalExpenses
        val avgMonthlyRevenue = totalRevenue / timeSeriesData.size
        val avgMonthlyExpenses = totalExpenses / timeSeriesData.size

        return mapOf(
            "totalAssets" to latest.assets,
            "totalLiabilities" to latest.liabilities,
            "totalEquity" to latest.equity,
            "totalRevenue" to totalRevenue,
            "totalExpenses" to totalExpenses,
            "netIncome" to netIncome,
            "avgMonthlyRevenue" to avgMonthlyRevenue,
            "avgMonthlyExpenses" to avgMonthlyExpenses,
            "currentMonth" to latest.month
        )
    }
}