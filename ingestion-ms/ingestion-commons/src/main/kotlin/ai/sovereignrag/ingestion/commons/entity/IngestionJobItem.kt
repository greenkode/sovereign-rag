package ai.sovereignrag.ingestion.commons.entity

import ai.sovereignrag.commons.model.AuditableEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "ingestion_job_items", schema = "ingestion")
class IngestionJobItem() : AuditableEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    var job: IngestionJob? = null

    var itemIndex: Int = 0

    @Column(length = 2000)
    var sourceReference: String? = null

    @Column(length = 500)
    var fileName: String? = null

    var fileSize: Long? = null

    @Column(length = 100)
    var mimeType: String? = null

    @Enumerated(EnumType.STRING)
    var status: JobStatus = JobStatus.PENDING

    @Column(length = 2000)
    var errorMessage: String? = null

    var chunksCreated: Int = 0

    var bytesProcessed: Long = 0

    constructor(
        job: IngestionJob,
        itemIndex: Int,
        sourceReference: String?,
        fileName: String? = null
    ) : this() {
        this.job = job
        this.itemIndex = itemIndex
        this.sourceReference = sourceReference
        this.fileName = fileName
    }

    fun markProcessing() {
        this.status = JobStatus.PROCESSING
    }

    fun markCompleted(chunksCreated: Int = 0, bytesProcessed: Long = 0) {
        this.status = JobStatus.COMPLETED
        this.chunksCreated = chunksCreated
        this.bytesProcessed = bytesProcessed
    }

    fun markFailed(errorMessage: String) {
        this.status = JobStatus.FAILED
        this.errorMessage = errorMessage
    }
}
