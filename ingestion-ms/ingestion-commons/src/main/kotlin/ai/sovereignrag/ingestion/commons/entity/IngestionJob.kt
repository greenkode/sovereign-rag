package ai.sovereignrag.ingestion.commons.entity

import ai.sovereignrag.commons.model.AuditableEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

enum class JobType {
    FILE_UPLOAD,
    WEB_SCRAPE,
    BATCH_IMPORT,
    FOLDER_IMPORT
}

enum class JobStatus {
    PENDING,
    UPLOADING,
    QUEUED,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELLED
}

enum class SourceType {
    S3_KEY,
    URL,
    ZIP_ARCHIVE,
    PRESIGNED_UPLOAD
}

@Entity
@Table(name = "ingestion_jobs", schema = "ingestion")
class IngestionJob() : AuditableEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null

    var organizationId: UUID = UUID.randomUUID()

    var knowledgeBaseId: UUID? = null

    @Enumerated(EnumType.STRING)
    var jobType: JobType = JobType.FILE_UPLOAD

    @Enumerated(EnumType.STRING)
    var status: JobStatus = JobStatus.PENDING

    @Enumerated(EnumType.STRING)
    var sourceType: SourceType? = null

    @Column(length = 2000)
    var sourceReference: String? = null

    @Column(length = 500)
    var fileName: String? = null

    var fileSize: Long? = null

    @Column(length = 100)
    var mimeType: String? = null

    var progress: Int = 0

    @Column(length = 2000)
    var errorMessage: String? = null

    var retryCount: Int = 0

    var maxRetries: Int = 3

    @Column(columnDefinition = "jsonb")
    var metadata: String? = null

    var startedAt: Instant? = null

    var completedAt: Instant? = null

    var processingDurationMs: Long? = null

    var chunksCreated: Int = 0

    var bytesProcessed: Long = 0

    var priority: Int = 0

    var lockedAt: Instant? = null

    var lockedBy: String? = null

    var visibleAfter: Instant? = null

    constructor(
        organizationId: UUID,
        jobType: JobType,
        knowledgeBaseId: UUID? = null,
        priority: Int = 0
    ) : this() {
        this.organizationId = organizationId
        this.jobType = jobType
        this.knowledgeBaseId = knowledgeBaseId
        this.priority = priority
    }

    fun markQueued() {
        this.status = JobStatus.QUEUED
    }

    fun markProcessing() {
        this.status = JobStatus.PROCESSING
        this.startedAt = Instant.now()
    }

    fun markCompleted(chunksCreated: Int = 0, bytesProcessed: Long = 0) {
        this.status = JobStatus.COMPLETED
        this.completedAt = Instant.now()
        this.progress = 100
        this.chunksCreated = chunksCreated
        this.bytesProcessed = bytesProcessed
        this.startedAt?.let { start ->
            this.processingDurationMs = Instant.now().toEpochMilli() - start.toEpochMilli()
        }
    }

    fun markFailed(errorMessage: String) {
        this.status = JobStatus.FAILED
        this.errorMessage = errorMessage
        this.completedAt = Instant.now()
        this.startedAt?.let { start ->
            this.processingDurationMs = Instant.now().toEpochMilli() - start.toEpochMilli()
        }
    }

    fun markCancelled() {
        this.status = JobStatus.CANCELLED
        this.completedAt = Instant.now()
    }

    fun canRetry(): Boolean {
        return retryCount < maxRetries && status == JobStatus.FAILED
    }

    fun incrementRetry() {
        retryCount++
        status = JobStatus.PENDING
        errorMessage = null
    }

    fun updateProgress(progress: Int) {
        this.progress = progress.coerceIn(0, 100)
    }

    fun createdAt(): Instant = createdAt ?: Instant.now()
}
