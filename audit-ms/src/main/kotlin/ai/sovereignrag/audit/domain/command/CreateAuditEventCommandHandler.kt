package ai.sovereignrag.audit.domain.command

import ai.sovereignrag.audit.config.MessageService
import ai.sovereignrag.audit.domain.model.AuditLog
import ai.sovereignrag.audit.domain.model.AuditLogEntity
import ai.sovereignrag.audit.domain.model.AuditLogRepository
import an.awesome.pipelinr.Command
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Component
class CreateAuditEventCommandHandler(
    private val auditLogRepository: AuditLogRepository,
    private val messageService: MessageService
) : Command.Handler<CreateAuditEventCommand, CreateAuditEventResult> {

    private val log = KotlinLogging.logger {}

    @Transactional(transactionManager = "mainTransactionManager")
    override fun handle(command: CreateAuditEventCommand): CreateAuditEventResult =
        runCatching {
            val ipAddress = extractIpAddress(command.payload)

            val auditLog = AuditLog(
                id = UUID.randomUUID(),
                actorId = command.actorId,
                actorName = command.actorName,
                merchantId = command.merchantId,
                resource = command.resource,
                event = command.event,
                eventTime = command.eventTime,
                timeRecorded = Instant.now(),
                identityType = command.identityType,
                payload = command.payload,
                ipAddress = ipAddress
            )

            val entity = AuditLogEntity.fromDomain(auditLog)
            val saved = auditLogRepository.save(entity)

            log.debug {
                "Audit event created: id=${saved.id}, identity=${command.actorId}, " +
                "event=${command.event}, resource=${command.resource}"
            }

            CreateAuditEventResult(
                id = saved.id,
                success = true,
                message = messageService.getMessage("audit.event.created")
            )
        }.getOrElse { e ->
            log.error(e) { "Failed to create audit event for identity=${command.actorId}, event=${command.event}" }

            CreateAuditEventResult(
                id = UUID.randomUUID(),
                success = false,
                message = messageService.getMessage("audit.event.create.failed")
            )
        }

    private fun extractIpAddress(payload: Map<String, Any>): String =
        payload["ipAddress"]?.toString() ?: "N/A"
}