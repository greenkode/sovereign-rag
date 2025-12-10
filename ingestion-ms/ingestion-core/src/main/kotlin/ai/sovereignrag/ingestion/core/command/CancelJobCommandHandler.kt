package ai.sovereignrag.ingestion.core.command

import ai.sovereignrag.ingestion.commons.audit.IngestionEventType
import ai.sovereignrag.ingestion.commons.entity.JobStatus
import ai.sovereignrag.ingestion.commons.repository.IngestionJobRepository
import ai.sovereignrag.ingestion.core.audit.IngestionAuditEventPublisher
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger {}

@Component
@Transactional
class CancelJobCommandHandler(
    private val jobRepository: IngestionJobRepository,
    private val messageSource: MessageSource,
    private val auditEventPublisher: IngestionAuditEventPublisher
) : Command.Handler<CancelJobCommand, CancelJobResult> {

    override fun handle(command: CancelJobCommand): CancelJobResult {
        log.info { "Processing CancelJobCommand for job ${command.jobId}" }

        val job = jobRepository.findById(command.jobId)
            .orElseThrow { IllegalArgumentException(getMessage("ingestion.error.job.not.found")) }

        require(job.organizationId == command.organizationId) {
            getMessage("ingestion.error.job.not.owned")
        }

        require(job.status in listOf(JobStatus.PENDING, JobStatus.UPLOADING, JobStatus.QUEUED)) {
            getMessage("ingestion.error.cannot.cancel", job.status.name)
        }

        job.markCancelled()
        jobRepository.save(job)

        log.info { "Cancelled job ${command.jobId} for organization ${command.organizationId}" }

        auditEventPublisher.publishJobInitiated(
            eventType = IngestionEventType.JOB_CANCELLED,
            organizationId = job.organizationId,
            jobId = job.id!!,
            knowledgeBaseId = job.knowledgeBaseId
        )

        return CancelJobResult(
            success = true,
            message = getMessage("ingestion.job.cancelled")
        )
    }

    private fun getMessage(code: String, vararg args: Any): String {
        return messageSource.getMessage(code, args, code, LocaleContextHolder.getLocale()) ?: code
    }
}
