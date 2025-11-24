package ai.sovereignrag.accounting.minigl.transaction.dto

import java.time.LocalDate

/**
 * DTO for daily limit reset statistics summary
 */
data class DailyLimitResetStatsSummary(
    val period: PeriodInfo,
    val execution: ExecutionMetrics,
    val performance: PerformanceMetrics,
    val accounts: AccountMetrics
)

data class PeriodInfo(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val days: Long
)

data class ExecutionMetrics(
    val totalRuns: Long,
    val successfulRuns: Long,
    val failedRuns: Long,
    val successRate: Double,
    val averageRunsPerDay: Double
)

data class PerformanceMetrics(
    val averageExecutionMs: Double,
    val minExecutionMs: Long,
    val maxExecutionMs: Long,
    val totalExecutionTimeMs: Long
)

data class AccountMetrics(
    val totalAccountsReset: Long,
    val averageAccountsPerRun: Double,
    val averageAccountsPerDay: Double
)

/**
 * DTO for average execution time summary
 */
data class AverageExecutionTimeSummary(
    val period: PeriodInfo,
    val metrics: AverageExecutionMetrics,
    val accounts: AccountMetrics
)

data class AverageExecutionMetrics(
    val averageExecutionTimeMs: Double,
    val averageExecutionTimeSeconds: Double,
    val minExecutionTimeMs: Long,
    val maxExecutionTimeMs: Long,
    val totalRuns: Long,
    val successfulRuns: Long,
    val failedRuns: Long,
    val successRate: Double
)

/**
 * DTO for performance time series data points
 */
data class PerformanceTimeSeriesDataPoint(
    val timestamp: Long,
    val value: Double,
    val label: String,
    val metadata: Map<String, Any>
)