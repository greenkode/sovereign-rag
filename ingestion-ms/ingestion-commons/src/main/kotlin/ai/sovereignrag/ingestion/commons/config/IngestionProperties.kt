package ai.sovereignrag.ingestion.commons.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "ingestion")
data class IngestionProperties(
    val limits: LimitsProperties = LimitsProperties(),
    val storage: StorageProperties = StorageProperties(),
    val scraping: ScrapingProperties = ScrapingProperties(),
    val processing: ProcessingProperties = ProcessingProperties(),
    val tiers: TierProperties = TierProperties(),
    val queue: QueueProperties = QueueProperties(),
    val embedding: EmbeddingProperties = EmbeddingProperties()
)

data class LimitsProperties(
    val maxFileSize: Long = 100 * 1024 * 1024,
    val maxConcurrentJobsPerOrganization: Int = 5,
    val maxBatchSize: Int = 50,
    val presignedUrlExpiryMinutes: Long = 15,
    val scrapeRateLimitPerMinute: Int = 10,
    val maxRetries: Int = 3,
    val jobTimeoutMinutes: Long = 60
)

data class TierProperties(
    val trial: TierLimits = TierLimits(
        storageQuotaBytes = 100 * 1024 * 1024,
        maxConcurrentJobs = 1,
        maxFileSize = 10 * 1024 * 1024,
        priority = 0,
        monthlyJobLimit = 50
    ),
    val starter: TierLimits = TierLimits(
        storageQuotaBytes = 1024L * 1024 * 1024,
        maxConcurrentJobs = 3,
        maxFileSize = 50 * 1024 * 1024,
        priority = 1,
        monthlyJobLimit = 500
    ),
    val professional: TierLimits = TierLimits(
        storageQuotaBytes = 10L * 1024 * 1024 * 1024,
        maxConcurrentJobs = 10,
        maxFileSize = 100 * 1024 * 1024,
        priority = 2,
        monthlyJobLimit = 5000
    ),
    val enterprise: TierLimits = TierLimits(
        storageQuotaBytes = Long.MAX_VALUE,
        maxConcurrentJobs = 50,
        maxFileSize = 500 * 1024 * 1024,
        priority = 3,
        monthlyJobLimit = Int.MAX_VALUE
    )
)

data class TierLimits(
    val storageQuotaBytes: Long,
    val maxConcurrentJobs: Int,
    val maxFileSize: Long,
    val priority: Int,
    val monthlyJobLimit: Int
)

data class QueueProperties(
    val type: QueueType = QueueType.POSTGRES,
    val pollIntervalMs: Long = 1000,
    val batchSize: Int = 10,
    val lockTimeoutMinutes: Long = 30,
    val visibilityTimeoutMinutes: Long = 5
)

enum class QueueType {
    POSTGRES,
    RABBITMQ,
    REDIS
}

data class StorageProperties(
    val rawBucket: String = "sovereign-rag-raw",
    val processedBucket: String = "sovereign-rag-processed",
    val tempPrefix: String = "temp/",
    val uploadsPrefix: String = "uploads/"
)

data class ScrapingProperties(
    val userAgent: String = "SovereignRAG-Bot/1.0",
    val timeoutSeconds: Int = 30,
    val maxContentLength: Long = 10 * 1024 * 1024,
    val respectRobotsTxt: Boolean = true,
    val delayBetweenRequestsMs: Long = 1000
)

data class ProcessingProperties(
    val chunkSize: Int = 1000,
    val chunkOverlap: Int = 200,
    val supportedMimeTypes: List<String> = listOf(
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.ms-powerpoint",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "text/plain",
        "text/markdown",
        "text/csv",
        "text/html",
        "application/json",
        "application/xml"
    )
)

data class EmbeddingProperties(
    val modelName: String = "nomic-embed-text",
    val ollamaBaseUrl: String = "http://localhost:11434",
    val openaiApiKey: String? = null,
    val huggingfaceApiKey: String? = null,
    val cohereApiKey: String? = null,
    val batchSize: Int = 32,
    val dimension: Int = 768,
    val maxRetries: Int = 3,
    val retryDelayMs: Long = 1000,
    val tablePrefix: String = "kb_",
    val tableSuffix: String = "_embeddings",
    val pgvector: PgVectorProperties = PgVectorProperties()
)

data class PgVectorProperties(
    val host: String = "localhost",
    val port: Int = 5432,
    val database: String = "sovereign_rag",
    val user: String = "postgres",
    val password: String = "postgres"
)
