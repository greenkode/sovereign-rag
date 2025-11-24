package ai.sovereignrag.accounting.minigl.transaction.service

import ai.sovereignrag.accounting.GLSession
import ai.sovereignrag.commons.performance.LogExecutionTime
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Alternative implementation that uses PostgreSQL stored procedure for daily limit reset.
 * This is more efficient for large datasets as it runs entirely in the database.
 */
@Service
class DailyLimitResetStoredProcedureService {

    private val log = KotlinLogging.logger {}

    @Autowired
    @Qualifier("accountingEntityManagerFactory")
    private lateinit var entityManagerFactory: EntityManagerFactory
    
    @Autowired
    private lateinit var glSession: GLSession
    
    private val entityManager: EntityManager
        get() = entityManagerFactory.createEntityManager()
    
    private val executorService = Executors.newScheduledThreadPool(1)
    
    fun startScheduledReset() {
        // Calculate initial delay to midnight
        val now = LocalDateTime.now()
        val midnight = now.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0)
        val initialDelay = java.time.Duration.between(now, midnight).seconds
        
        log.info("Scheduling daily limit reset (stored procedure) to run at midnight. Initial delay: $initialDelay seconds")
        
        // Schedule to run at midnight every day
        executorService.scheduleAtFixedRate(
            { runDailyLimitResetStoredProcedure() },
            initialDelay,
            TimeUnit.DAYS.toSeconds(1),
            TimeUnit.SECONDS
        )
    }
    
    @LogExecutionTime
    @Transactional(transactionManager = "accountingTransactionManager")
    fun runDailyLimitResetStoredProcedure() {
        log.info("Starting daily limit reset using stored procedure at ${LocalDateTime.now()}")
        
        try {
            val session = entityManager.unwrap(org.hibernate.Session::class.java)
            val query = session.createNativeQuery("SELECT reset_daily_limits()")
            val result = query.singleResult as Number
            val resetCount = result.toInt()
            
            log.info("Daily limit reset completed successfully. Reset $resetCount accounts")
        } catch (e: Exception) {
            log.error("Error during daily limit reset using stored procedure", e)
        }
    }
    
    /**
     * Check if daily reset has been run today and run if needed
     */
    @LogExecutionTime
    fun checkAndRunIfNeeded() {
        log.info("Checking if daily limit reset is needed for today")
        
        try {
            val session = entityManager.unwrap(org.hibernate.Session::class.java)
            val query = session.createNativeQuery("""
                SELECT status, accounts_reset, last_run_time
                FROM daily_limit_reset_today
            """)
            
            @Suppress("UNCHECKED_CAST")
            val results = query.resultList as List<Array<Any>>
            
            if (results.isEmpty() || results[0][0] == "NOT_RUN") {
                log.info("Daily limit reset has not been run today. Triggering reset now.")
                runDailyLimitResetStoredProcedure()
            } else {
                val status = results[0][0] as String
                val accountsReset = results[0][1] as Number
                val lastRunTime = results[0][2]
                log.info("Daily limit reset already completed today. Status: $status, Accounts: $accountsReset, Time: $lastRunTime")
            }
            
        } catch (e: Exception) {
            log.error("Error checking daily limit reset status", e)
            // If we can't check status, try to run anyway
            log.info("Unable to check status, attempting to run daily limit reset")
            runDailyLimitResetStoredProcedure()
        }
    }
    
    /**
     * Get the status of recent daily limit resets for monitoring
     */
    @LogExecutionTime
    fun getDailyLimitResetStatus(): List<Map<String, Any>> {
        return try {
            val session = entityManager.unwrap(org.hibernate.Session::class.java)
            val query = session.createNativeQuery("""
                SELECT 
                    transaction_detail,
                    reset_timestamp,
                    reset_date,
                    entries_count,
                    total_amount_reset
                FROM daily_limit_reset_status
                LIMIT 100
            """)
            
            @Suppress("UNCHECKED_CAST")
            val results = query.resultList as List<Array<Any>>
            
            results.map { row ->
                mapOf(
                    "transactionDetail" to row[0],
                    "resetTimestamp" to row[1],
                    "resetDate" to row[2],
                    "entriesCount" to row[3],
                    "totalAmountReset" to row[4]
                )
            }
            
        } catch (e: Exception) {
            log.error("Error retrieving daily limit reset status", e)
            emptyList()
        }
    }
    
    /**
     * Manual trigger for daily limit reset (for testing or emergency use)
     */
    @LogExecutionTime
    @Transactional(transactionManager = "accountingTransactionManager")
    fun manualReset(): Int {
        log.info("Manual daily limit reset triggered at ${LocalDateTime.now()}")
        
        return try {
            val session = entityManager.unwrap(org.hibernate.Session::class.java)
            val query = session.createNativeQuery("SELECT reset_daily_limits()")
            val result = query.singleResult as Number
            val resetCount = result.toInt()
            
            log.info("Manual daily limit reset completed. Reset $resetCount accounts")
            resetCount
        } catch (e: Exception) {
            log.error("Error during manual daily limit reset", e)
            throw e
        }
    }
    
    fun shutdown() {
        executorService.shutdown()
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executorService.shutdownNow()
        }
    }
}