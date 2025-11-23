package ai.sovereignrag.identity.process.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync

/**
 * Configuration properties for process management
 * Full migration to orchestrator-based architecture - no legacy support
 */
@Configuration
@EnableAsync
@ConfigurationProperties(prefix = "bml.process")
data class ProcessConfiguration(
    /**
     * Event processing configuration
     */
    var eventProcessing: EventProcessingConfig = EventProcessingConfig(),
    
    /**
     * Process execution configuration
     */
    var execution: ProcessExecutionConfig = ProcessExecutionConfig()
)

data class EventProcessingConfig(
    /**
     * Whether to validate state transitions (default: true)
     */
    var validateTransitions: Boolean = true,
    
    /**
     * Whether to publish state change events (default: true)
     */
    var publishStateChangeEvents: Boolean = true,
    
    /**
     * Whether to enable audit logging for state changes (default: true)
     */
    var auditStateChanges: Boolean = true,
    
    /**
     * Whether to enable async event processing (default: true)
     */
    var asyncEventProcessing: Boolean = true
)

data class ProcessExecutionConfig(
    /**
     * Maximum number of retry attempts for failed processes (default: 3)
     */
    var maxRetryAttempts: Int = 3,
    
    /**
     * Timeout for process execution in milliseconds (default: 30000)
     */
    var executionTimeoutMs: Long = 30000,
    
    /**
     * Whether to enable parallel processing of events (default: false)
     */
    var enableParallelProcessing: Boolean = false,
    
    /**
     * Process cleanup configuration
     */
    var cleanup: ProcessCleanupConfig = ProcessCleanupConfig()
)

data class ProcessCleanupConfig(
    /**
     * Whether to enable automatic cleanup of completed processes (default: true)
     */
    var enabled: Boolean = false,
    
    /**
     * Number of days to retain completed processes (default: 30)
     */
    var retentionDays: Int = 30,
    
    /**
     * Number of days to retain failed processes (default: 90)
     */
    var failedRetentionDays: Int = 90
)