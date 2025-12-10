package ai.sovereignrag.ingestion.core.audit

import ai.sovereignrag.ingestion.commons.audit.IngestionAuditEvent
import ai.sovereignrag.ingestion.commons.audit.IngestionAuditPayloadKey
import ai.sovereignrag.ingestion.commons.audit.IngestionAuditResource
import ai.sovereignrag.ingestion.commons.audit.IngestionEventType
import ai.sovereignrag.ingestion.commons.audit.IngestionIdentityType
import ai.sovereignrag.ingestion.commons.entity.IngestionJob
import ai.sovereignrag.ingestion.core.service.SecurityContextService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

private val log = KotlinLogging.logger {}

@Component
class IngestionAuditEventPublisher(
    private val eventPublisher: ApplicationEventPublisher,
    private val securityContextService: SecurityContextService
) {

    fun publishJobInitiated(
        eventType: IngestionEventType,
        organizationId: UUID,
        jobId: UUID,
        knowledgeBaseId: UUID?,
        additionalPayload: Map<String, String> = emptyMap()
    ) {
        val actorInfo = getActorInfo()
        val payload = buildBasePayload(jobId, knowledgeBaseId) + additionalPayload

        publish(
            IngestionAuditEvent(
                actorId = actorInfo.actorId,
                actorName = actorInfo.actorName,
                organizationId = organizationId,
                identityType = actorInfo.identityType,
                resource = IngestionAuditResource.INGESTION,
                event = eventType,
                eventTime = Instant.now(),
                jobId = jobId,
                knowledgeBaseId = knowledgeBaseId,
                payload = payload
            )
        )
    }

    fun publishJobCompleted(job: IngestionJob, eventType: IngestionEventType) {
        val actorInfo = getActorInfo()
        val payload = buildJobCompletionPayload(job)

        publish(
            IngestionAuditEvent(
                actorId = actorInfo.actorId,
                actorName = actorInfo.actorName,
                organizationId = job.organizationId,
                identityType = actorInfo.identityType,
                resource = IngestionAuditResource.INGESTION,
                event = eventType,
                eventTime = Instant.now(),
                jobId = job.id,
                knowledgeBaseId = job.knowledgeBaseId,
                payload = payload
            )
        )
    }

    fun publishJobFailed(job: IngestionJob, eventType: IngestionEventType, errorMessage: String?) {
        val actorInfo = getActorInfo()
        val payload = buildJobCompletionPayload(job) +
            (errorMessage?.let { mapOf(IngestionAuditPayloadKey.ERROR_MESSAGE.value to it) } ?: emptyMap())

        publish(
            IngestionAuditEvent(
                actorId = actorInfo.actorId,
                actorName = actorInfo.actorName,
                organizationId = job.organizationId,
                identityType = actorInfo.identityType,
                resource = IngestionAuditResource.INGESTION,
                event = eventType,
                eventTime = Instant.now(),
                jobId = job.id,
                knowledgeBaseId = job.knowledgeBaseId,
                payload = payload
            )
        )
    }

    fun publishKnowledgeSourceEvent(
        eventType: IngestionEventType,
        organizationId: UUID,
        knowledgeBaseId: UUID,
        knowledgeSourceId: UUID,
        additionalPayload: Map<String, String> = emptyMap()
    ) {
        val actorInfo = getActorInfo()
        val payload = mapOf(
            IngestionAuditPayloadKey.KNOWLEDGE_SOURCE_ID.value to knowledgeSourceId.toString(),
            IngestionAuditPayloadKey.KNOWLEDGE_BASE_ID.value to knowledgeBaseId.toString()
        ) + additionalPayload

        publish(
            IngestionAuditEvent(
                actorId = actorInfo.actorId,
                actorName = actorInfo.actorName,
                organizationId = organizationId,
                identityType = actorInfo.identityType,
                resource = IngestionAuditResource.KNOWLEDGE_SOURCE,
                event = eventType,
                eventTime = Instant.now(),
                knowledgeBaseId = knowledgeBaseId,
                payload = payload
            )
        )
    }

    fun publishCustomEvent(
        eventType: IngestionEventType,
        organizationId: UUID,
        resource: IngestionAuditResource = IngestionAuditResource.INGESTION,
        jobId: UUID? = null,
        knowledgeBaseId: UUID? = null,
        payload: Map<String, String> = emptyMap()
    ) {
        val actorInfo = getActorInfo()

        publish(
            IngestionAuditEvent(
                actorId = actorInfo.actorId,
                actorName = actorInfo.actorName,
                organizationId = organizationId,
                identityType = actorInfo.identityType,
                resource = resource,
                event = eventType,
                eventTime = Instant.now(),
                jobId = jobId,
                knowledgeBaseId = knowledgeBaseId,
                payload = payload
            )
        )
    }

    private fun publish(event: IngestionAuditEvent) {
        log.debug { "Publishing audit event: ${event.event} for organization ${event.organizationId}" }
        eventPublisher.publishEvent(event)
    }

    private fun getActorInfo(): ActorInfo {
        return runCatching {
            val userId = securityContextService.getCurrentUserId()
            val userName = securityContextService.getCurrentUserName()
            ActorInfo(
                actorId = userId.toString(),
                actorName = userName,
                identityType = IngestionIdentityType.USER
            )
        }.getOrElse {
            ActorInfo(
                actorId = "SYSTEM",
                actorName = "System",
                identityType = IngestionIdentityType.SYSTEM
            )
        }
    }

    private fun buildBasePayload(jobId: UUID, knowledgeBaseId: UUID?): Map<String, String> {
        val payload = mutableMapOf(
            IngestionAuditPayloadKey.JOB_ID.value to jobId.toString()
        )
        knowledgeBaseId?.let {
            payload[IngestionAuditPayloadKey.KNOWLEDGE_BASE_ID.value] = it.toString()
        }
        return payload
    }

    private fun buildJobCompletionPayload(job: IngestionJob): Map<String, String> {
        val payload = mutableMapOf(
            IngestionAuditPayloadKey.JOB_ID.value to (job.id?.toString() ?: ""),
            IngestionAuditPayloadKey.JOB_TYPE.value to job.jobType.name,
            IngestionAuditPayloadKey.CHUNKS_CREATED.value to job.chunksCreated.toString(),
            IngestionAuditPayloadKey.BYTES_PROCESSED.value to job.bytesProcessed.toString()
        )
        job.knowledgeBaseId?.let {
            payload[IngestionAuditPayloadKey.KNOWLEDGE_BASE_ID.value] = it.toString()
        }
        job.fileName?.let {
            payload[IngestionAuditPayloadKey.FILE_NAME.value] = it
        }
        job.fileSize?.let {
            payload[IngestionAuditPayloadKey.FILE_SIZE.value] = it.toString()
        }
        job.mimeType?.let {
            payload[IngestionAuditPayloadKey.MIME_TYPE.value] = it
        }
        job.processingDurationMs?.let {
            payload[IngestionAuditPayloadKey.PROCESSING_DURATION_MS.value] = it.toString()
        }
        return payload
    }

    private data class ActorInfo(
        val actorId: String,
        val actorName: String,
        val identityType: IngestionIdentityType
    )
}
