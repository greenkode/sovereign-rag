package ai.sovereignrag.audit.domain.command

import an.awesome.pipelinr.Command
import java.time.Instant
import java.util.UUID

data class CreateAuditEventCommand(
    val actorId: String,
    val actorName: String,
    val merchantId: String,
    val identityType: String,
    val resource: String,
    val event: String,
    val eventTime: Instant,
    val payload: Map<String, Any>
) : Command<CreateAuditEventResult>

data class CreateAuditEventResult(
    val id: UUID,
    val success: Boolean,
    val message: String? = null
)