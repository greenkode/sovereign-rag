package ai.sovereignrag.ingestion.commons.repository

import ai.sovereignrag.ingestion.commons.entity.IngestionJob
import ai.sovereignrag.ingestion.commons.entity.IngestionJobItem
import ai.sovereignrag.ingestion.commons.entity.JobStatus
import ai.sovereignrag.ingestion.commons.entity.OrganizationQuota
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
interface IngestionJobRepository : JpaRepository<IngestionJob, UUID> {

    fun findByOrganizationIdOrderByCreatedAtDesc(organizationId: UUID, pageable: Pageable): Page<IngestionJob>

    fun findByOrganizationIdAndStatusOrderByCreatedAtDesc(organizationId: UUID, status: JobStatus, pageable: Pageable): Page<IngestionJob>

    fun findByOrganizationIdAndKnowledgeBaseIdOrderByCreatedAtDesc(organizationId: UUID, knowledgeBaseId: UUID, pageable: Pageable): Page<IngestionJob>

    @Query("SELECT j FROM IngestionJob j WHERE j.status = :status ORDER BY j.createdAt ASC")
    fun findPendingJobs(status: JobStatus, pageable: Pageable): Page<IngestionJob>

    @Query("SELECT COUNT(j) FROM IngestionJob j WHERE j.organizationId = :organizationId AND j.status IN :statuses")
    fun countActiveJobsForOrganization(organizationId: UUID, statuses: List<JobStatus>): Long

    @Query("SELECT j FROM IngestionJob j WHERE j.status = :status AND j.retryCount < j.maxRetries ORDER BY j.createdAt ASC")
    fun findRetryableJobs(status: JobStatus, pageable: Pageable): Page<IngestionJob>

    @Modifying
    @Query("UPDATE IngestionJob j SET j.status = :newStatus WHERE j.status = :oldStatus AND j.createdAt < :before")
    fun markStaleJobsAsFailed(oldStatus: JobStatus, newStatus: JobStatus, before: Instant): Int

    @Query("SELECT SUM(j.fileSize) FROM IngestionJob j WHERE j.organizationId = :organizationId AND j.status = 'COMPLETED'")
    fun getTotalBytesProcessedForOrganization(organizationId: UUID): Long?

    @Modifying
    @Query("""
        UPDATE ingestion.ingestion_jobs
        SET status = 'PROCESSING',
            locked_at = :lockTime,
            locked_by = :workerId,
            started_at = :lockTime
        WHERE id = (
            SELECT id FROM ingestion.ingestion_jobs
            WHERE status = 'QUEUED'
            AND (visible_after IS NULL OR visible_after <= :lockTime)
            ORDER BY priority DESC, created_date ASC
            LIMIT 1
            FOR UPDATE SKIP LOCKED
        )
    """, nativeQuery = true)
    fun claimNextJob(lockTime: Instant, workerId: String): Int

    @Modifying
    @Query("""
        UPDATE IngestionJob j
        SET j.status = 'QUEUED',
            j.lockedAt = NULL,
            j.lockedBy = NULL,
            j.visibleAfter = :visibleAfter
        WHERE j.status = 'PROCESSING'
        AND j.lockedAt < :staleThreshold
    """)
    fun releaseStaleJobs(staleThreshold: Instant, visibleAfter: Instant): Int

    @Query("""
        SELECT j FROM IngestionJob j
        WHERE j.status = 'QUEUED'
        AND (j.visibleAfter IS NULL OR j.visibleAfter <= :now)
        ORDER BY j.priority DESC, j.createdAt ASC
    """)
    fun findQueuedJobsByPriority(now: Instant, pageable: Pageable): Page<IngestionJob>

    @Query("SELECT COUNT(j) FROM IngestionJob j WHERE j.organizationId = :organizationId AND j.status = 'COMPLETED' AND j.completedAt >= :since")
    fun countCompletedJobsSince(organizationId: UUID, since: Instant): Long
}

@Repository
interface OrganizationQuotaRepository : JpaRepository<OrganizationQuota, UUID> {

    @Query("SELECT q FROM OrganizationQuota q WHERE q.organizationId = :organizationId")
    fun findByOrganizationId(organizationId: UUID): OrganizationQuota?

    @Modifying
    @Query("UPDATE OrganizationQuota q SET q.storageUsedBytes = q.storageUsedBytes + :bytes WHERE q.organizationId = :organizationId")
    fun incrementStorageUsed(organizationId: UUID, bytes: Long): Int

    @Modifying
    @Query("UPDATE OrganizationQuota q SET q.monthlyJobsUsed = q.monthlyJobsUsed + 1, q.lastJobCompletedAt = :completedAt WHERE q.organizationId = :organizationId")
    fun incrementMonthlyJobCount(organizationId: UUID, completedAt: Instant): Int

    @Query("SELECT q FROM OrganizationQuota q WHERE q.currentPeriodStart < :resetThreshold")
    fun findQuotasNeedingMonthlyReset(resetThreshold: Instant): List<OrganizationQuota>
}

@Repository
interface IngestionJobItemRepository : JpaRepository<IngestionJobItem, UUID> {

    fun findByJobIdOrderByItemIndex(jobId: UUID): List<IngestionJobItem>

    fun findByJobIdAndStatus(jobId: UUID, status: JobStatus): List<IngestionJobItem>

    @Query("SELECT COUNT(i) FROM IngestionJobItem i WHERE i.job.id = :jobId AND i.status = :status")
    fun countByJobIdAndStatus(jobId: UUID, status: JobStatus): Long

    @Query("SELECT COUNT(i) FROM IngestionJobItem i WHERE i.job.id = :jobId")
    fun countByJobId(jobId: UUID): Long
}
