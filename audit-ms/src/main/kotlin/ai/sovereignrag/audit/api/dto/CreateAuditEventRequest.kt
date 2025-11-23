package ai.sovereignrag.audit.api.dto

import ai.sovereignrag.audit.domain.command.CreateAuditEventCommand
import java.time.Instant

data class CreateAuditEventRequest(
    val actorId: String,
    val actorName: String,
    val merchantId: String,
    val identityType: String,
    val resource: String,
    val event: String,
    val eventTime: Instant,
    val payload: Map<String, Any>
) {
    fun toCommand(): CreateAuditEventCommand {
        return CreateAuditEventCommand(actorId, actorName, merchantId, identityType, resource, event, eventTime, payload)
    }
}

data class CreateAuditEventResponse(
    val success: Boolean,
    val message: String? = null
)