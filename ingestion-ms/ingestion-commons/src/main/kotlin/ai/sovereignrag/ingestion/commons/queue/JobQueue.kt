package ai.sovereignrag.ingestion.commons.queue

import ai.sovereignrag.ingestion.commons.entity.IngestionJob
import java.util.UUID

interface JobQueue {
    fun enqueue(job: IngestionJob)
    fun dequeue(workerId: String): IngestionJob?
    fun complete(jobId: UUID)
    fun fail(jobId: UUID, errorMessage: String)
    fun retry(jobId: UUID)
    fun releaseStaleJobs()
    fun getQueueDepth(): Long
    fun getQueueDepthByPriority(): Map<Int, Long>
}
