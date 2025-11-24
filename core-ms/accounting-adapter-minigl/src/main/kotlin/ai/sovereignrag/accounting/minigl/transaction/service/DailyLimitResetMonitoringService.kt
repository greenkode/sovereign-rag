package ai.sovereignrag.accounting.minigl.transaction.service

import ai.sovereignrag.accounting.minigl.transaction.dao.DailyLimitResetStatsRepository
import ai.sovereignrag.accounting.minigl.transaction.dto.AverageExecutionMetrics
import ai.sovereignrag.accounting.minigl.transaction.dto.AverageExecutionTimeSummary
import ai.sovereignrag.accounting.minigl.transaction.dto.DailyLimitResetStatsSummary
import ai.sovereignrag.accounting.minigl.transaction.dto.PerformanceTimeSeriesDataPoint
import ai.sovereignrag.accounting.minigl.transaction.model.DailyLimitResetStats
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DailyLimitResetMonitoringService(
    private val statsRepository: DailyLimitResetStatsRepository
) {
    
    private val logger = LoggerFactory.getLogger(DailyLimitResetMonitoringService::class.java)
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    
    /**
     * Get time series data for daily limit reset performance
     */
    fun getTimeSeriesData(startDate: LocalDate, endDate: LocalDate): List<PerformanceTimeSeriesDataPoint> {
        logger.info("Fetching time series data from $startDate to $endDate")
        
        val rawData = statsRepository.getTimeSeriesData(startDate, endDate)
        
        return rawData.map { data ->
            PerformanceTimeSeriesDataPoint(
                timestamp = data["timestamp"] as Long,
                value = (data["executionTime"] as Long).toDouble(),
                label = data["date"].toString(),
                metadata = mapOf<String, Any>(
                    "accountsReset" to (data["accountsReset"] ?: 0),
                    "status" to (data["status"] ?: "UNKNOWN")
                )
            )
        }
    }
    
    /**
     * Get execution history within a date range
     */
    fun getExecutionHistory(startDate: LocalDate, endDate: LocalDate): List<Map<String, Any>> {
        logger.info("Fetching execution history from $startDate to $endDate")
        
        val stats = statsRepository.findByExecutionDateBetween(startDate, endDate)
        
        return stats.map { stat ->
            mapOf<String, Any>(
                "id" to (stat.id ?: 0L),
                "date" to stat.executionDate.format(dateFormatter),
                "startTime" to stat.startTime.toString(),
                "endTime" to stat.endTime.toString(),
                "duration" to (stat.executionTimeMs ?: 0),
                "accountsReset" to stat.accountsReset,
                "status" to stat.status,
                "errorMessage" to (stat.errorMessage ?: ""),
                "success" to (stat.status in listOf(
                    DailyLimitResetStats.STATUS_SUCCESS,
                    DailyLimitResetStats.STATUS_SUCCESS_NO_ACCOUNTS
                ))
            )
        }
    }
    
    /**
     * Calculate average execution time for a period
     */
    fun getAverageExecutionTime(startDate: LocalDate, endDate: LocalDate): AverageExecutionTimeSummary {
        logger.info("Calculating average execution time from $startDate to $endDate")
        
        val avgTime = statsRepository.calculateAverageExecutionTime(startDate, endDate)
        val summary = statsRepository.getStatsSummary(startDate, endDate)
        
        return AverageExecutionTimeSummary(
            period = summary.period,
            metrics = AverageExecutionMetrics(
                averageExecutionTimeMs = avgTime,
                averageExecutionTimeSeconds = avgTime / 1000.0,
                minExecutionTimeMs = summary.performance.minExecutionMs,
                maxExecutionTimeMs = summary.performance.maxExecutionMs,
                totalRuns = summary.execution.totalRuns,
                successfulRuns = summary.execution.successfulRuns,
                failedRuns = summary.execution.failedRuns,
                successRate = summary.execution.successRate
            ),
            accounts = summary.accounts
        )
    }
    
    /**
     * Get performance summary for a period
     */
    fun getPerformanceSummary(startDate: LocalDate, endDate: LocalDate): DailyLimitResetStatsSummary {
        logger.info("Getting performance summary from $startDate to $endDate")
        
        return statsRepository.getStatsSummary(startDate, endDate)
    }
    
    /**
     * Get today's reset status
     */
    fun getTodayStatus(): Map<String, Any> {
        logger.info("Getting today's daily limit reset status")
        
        val todayStats = statsRepository.findTodaysStats()
        
        return if (todayStats != null) {
            mapOf<String, Any>(
                "hasRun" to true,
                "status" to todayStats.status,
                "executionTime" to (todayStats.executionTimeMs ?: 0),
                "accountsReset" to todayStats.accountsReset,
                "startTime" to todayStats.startTime.toString(),
                "endTime" to todayStats.endTime.toString(),
                "errorMessage" to (todayStats.errorMessage ?: "")
            )
        } else {
            mapOf<String, Any>(
                "hasRun" to false,
                "status" to "NOT_RUN",
                "message" to "Daily limit reset has not been executed today"
            )
        }
    }
}