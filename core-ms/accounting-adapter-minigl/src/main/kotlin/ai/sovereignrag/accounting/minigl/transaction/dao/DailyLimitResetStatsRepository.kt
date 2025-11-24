package ai.sovereignrag.accounting.minigl.transaction.dao

import ai.sovereignrag.accounting.minigl.transaction.dto.AccountMetrics
import ai.sovereignrag.accounting.minigl.transaction.dto.DailyLimitResetStatsSummary
import ai.sovereignrag.accounting.minigl.transaction.dto.ExecutionMetrics
import ai.sovereignrag.accounting.minigl.transaction.dto.PerformanceMetrics
import ai.sovereignrag.accounting.minigl.transaction.dto.PeriodInfo
import ai.sovereignrag.accounting.minigl.transaction.model.DailyLimitResetStats
import ai.sovereignrag.commons.performance.LogExecutionTime
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class DailyLimitResetStatsRepository {

    private val log = KotlinLogging.logger {}

    @Autowired
    @Qualifier("accountingEntityManagerFactory")
    private lateinit var entityManagerFactory: EntityManagerFactory
    
    private val entityManager: EntityManager
        get() = entityManagerFactory.createEntityManager()

    
    /**
     * Find stats within a date range
     */
    @LogExecutionTime
    fun findByExecutionDateBetween(startDate: LocalDate, endDate: LocalDate): List<DailyLimitResetStats> {
        val query = entityManager.createQuery(
            """FROM DailyLimitResetStats s 
               WHERE s.executionDate >= :startDate 
               AND s.executionDate <= :endDate 
               ORDER BY s.executionDate DESC""",
            DailyLimitResetStats::class.java
        )
        query.setParameter("startDate", startDate)
        query.setParameter("endDate", endDate)
        return query.resultList
    }
    
    /**
     * Calculate average execution time for a period
     */
    @LogExecutionTime
    fun calculateAverageExecutionTime(startDate: LocalDate, endDate: LocalDate): Double {
        val query = entityManager.createQuery(
            """SELECT AVG(s.executionTimeMs) 
               FROM DailyLimitResetStats s 
               WHERE s.executionDate >= :startDate 
               AND s.executionDate <= :endDate 
               AND s.status IN (:successStatuses)""",
            java.lang.Double::class.java
        )
        query.setParameter("startDate", startDate)
        query.setParameter("endDate", endDate)
        query.setParameter("successStatuses", listOf(
            DailyLimitResetStats.STATUS_SUCCESS,
            DailyLimitResetStats.STATUS_SUCCESS_NO_ACCOUNTS
        ))
        return query.singleResult?.toDouble() ?: 0.0
    }
    
    /**
     * Get time series data for performance monitoring
     */
    @LogExecutionTime
    fun getTimeSeriesData(startDate: LocalDate, endDate: LocalDate): List<Map<String, Any>> {
        val query = entityManager.createNativeQuery(
            """SELECT 
                execution_date as date,
                execution_time_ms as executionTime,
                accounts_reset as accountsReset,
                status,
                EXTRACT(EPOCH FROM start_time) * 1000 as timestamp
               FROM accounting.daily_limit_reset_stats
               WHERE execution_date >= :startDate 
               AND execution_date <= :endDate
               ORDER BY execution_date ASC"""
        )
        query.setParameter("startDate", startDate)
        query.setParameter("endDate", endDate)
        
        @Suppress("UNCHECKED_CAST")
        val results = query.resultList as List<Array<Any>>
        
        return results.map { row ->
            mapOf(
                "date" to row[0],
                "executionTime" to (row[1] ?: 0L),
                "accountsReset" to (row[2] ?: 0),
                "status" to row[3],
                "timestamp" to (row[4] as Number).toLong()
            )
        }
    }
    
    /**
     * Get statistics summary for a period
     */
    @LogExecutionTime
    fun getStatsSummary(startDate: LocalDate, endDate: LocalDate): DailyLimitResetStatsSummary {
            val query = entityManager.createNativeQuery(
                """SELECT 
                    COUNT(*) as totalRuns,
                    COUNT(CASE WHEN status IN ('SUCCESS', 'SUCCESS_NO_ACCOUNTS') THEN 1 END) as successfulRuns,
                    COUNT(CASE WHEN status = 'FAILED' THEN 1 END) as failedRuns,
                    AVG(CASE WHEN status = 'SUCCESS' THEN execution_time_ms END) as avgExecutionTime,
                    MIN(execution_time_ms) as minExecutionTime,
                    MAX(execution_time_ms) as maxExecutionTime,
                    AVG(CASE WHEN status = 'SUCCESS' THEN accounts_reset END) as avgAccountsReset,
                    SUM(CASE WHEN status = 'SUCCESS' THEN accounts_reset ELSE 0 END) as totalAccountsReset
                   FROM accounting.daily_limit_reset_stats
                   WHERE execution_date >= :startDate 
                   AND execution_date <= :endDate"""
            )
            query.setParameter("startDate", startDate)
            query.setParameter("endDate", endDate)
            
            val result = query.singleResult as Array<Any>
            
            // Extract values from query result
            val totalRuns = (result[0] as? Number)?.toLong() ?: 0L
            val successfulRuns = (result[1] as? Number)?.toLong() ?: 0L
            val failedRuns = (result[2] as? Number)?.toLong() ?: 0L
            val avgExecutionTime = (result[3] as? Number)?.toDouble() ?: 0.0
            val minExecutionTime = (result[4] as? Number)?.toLong() ?: 0L
            val maxExecutionTime = (result[5] as? Number)?.toLong() ?: 0L
            val avgAccountsReset = (result[6] as? Number)?.toDouble() ?: 0.0
            val totalAccountsReset = (result[7] as? Number)?.toLong() ?: 0L
            
            // Calculate additional metrics
            val days = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1
            val averageRunsPerDay = totalRuns.toDouble() / days
            val averageAccountsPerDay = totalAccountsReset.toDouble() / days
            val successRate = if (totalRuns > 0) (successfulRuns.toDouble() / totalRuns) * 100 else 0.0
            
            // Get total execution time from all records
            val totalExecutionTimeQuery = entityManager.createNativeQuery(
                """SELECT SUM(execution_time_ms) 
                   FROM accounting.daily_limit_reset_stats
                   WHERE execution_date >= :startDate 
                   AND execution_date <= :endDate
                   AND status = 'SUCCESS'"""
            )
            totalExecutionTimeQuery.setParameter("startDate", startDate)
            totalExecutionTimeQuery.setParameter("endDate", endDate)
            val totalExecutionTime = (totalExecutionTimeQuery.singleResult as? Number)?.toLong() ?: 0L

            return DailyLimitResetStatsSummary(
                period = PeriodInfo(
                    startDate = startDate,
                    endDate = endDate,
                    days = days
                ),
                execution = ExecutionMetrics(
                    totalRuns = totalRuns,
                    successfulRuns = successfulRuns,
                    failedRuns = failedRuns,
                    successRate = successRate,
                    averageRunsPerDay = averageRunsPerDay
                ),
                performance = PerformanceMetrics(
                    averageExecutionMs = avgExecutionTime,
                    minExecutionMs = minExecutionTime,
                    maxExecutionMs = maxExecutionTime,
                    totalExecutionTimeMs = totalExecutionTime
                ),
                accounts = AccountMetrics(
                    totalAccountsReset = totalAccountsReset,
                    averageAccountsPerRun = avgAccountsReset,
                    averageAccountsPerDay = averageAccountsPerDay
                )
            )
    }
    
    /**
     * Find today's stats
     */
    @LogExecutionTime
    fun findTodaysStats(): DailyLimitResetStats? {
        val query = entityManager.createQuery(
            """FROM DailyLimitResetStats s 
               WHERE s.executionDate = :today 
               ORDER BY s.startTime DESC""",
            DailyLimitResetStats::class.java
        )
        query.setParameter("today", LocalDate.now())
        query.maxResults = 1
        return query.resultList.firstOrNull()
    }
}