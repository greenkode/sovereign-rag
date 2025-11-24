package ai.sovereignrag.accounting.minigl.transaction.service

import org.slf4j.LoggerFactory

/**
 * Configuration class for daily limit reset functionality using PostgreSQL stored procedure.
 */
class DailyLimitResetConfiguration {
    
    private val logger = LoggerFactory.getLogger(DailyLimitResetConfiguration::class.java)
    
    private var storedProcedureService: DailyLimitResetStoredProcedureService? = null
    
    fun initialize() {
        val enableScheduling = System.getProperty("daily.limit.reset.enabled", "true").toBoolean()
        
        if (!enableScheduling) {
            logger.info("Daily limit reset scheduling is disabled")
            return
        }
        
        logger.info("Initializing daily limit reset with PostgreSQL stored procedure")
        storedProcedureService = DailyLimitResetStoredProcedureService()
        storedProcedureService?.startScheduledReset()
        
        // Check if daily reset has been run today
        storedProcedureService?.checkAndRunIfNeeded()
        
        logger.info("Daily limit reset service initialized successfully")
    }
    
    fun shutdown() {
        logger.info("Shutting down daily limit reset service")
        storedProcedureService?.shutdown()
    }
    
    /**
     * Manual trigger for daily limit reset (useful for testing or emergency situations)
     */
    fun triggerManualReset(): Int {
        return storedProcedureService?.manualReset()
            ?: throw IllegalStateException("Daily limit reset service is not initialized")
    }
    
    /**
     * Get status of recent daily limit resets
     */
    fun getResetStatus(): List<Map<String, Any>> {
        return storedProcedureService?.getDailyLimitResetStatus() ?: emptyList()
    }
    
    /**
     * Check if the service is using stored procedure implementation (always true now)
     */
    fun isUsingStoredProcedure(): Boolean = true
}