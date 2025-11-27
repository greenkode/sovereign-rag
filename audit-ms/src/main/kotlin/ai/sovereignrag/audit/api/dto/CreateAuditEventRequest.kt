package ai.sovereignrag.audit.api.dto

import ai.sovereignrag.audit.domain.command.CreateAuditEventCommand
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

data class CreateAuditEventRequest(
    @JsonProperty("actor_id") val actorId: String,
    @JsonProperty("actor_name") val actorName: String,
    @JsonProperty("merchant_id") val merchantId: String,
    @JsonProperty("identity_type") val identityType: String,
    @JsonProperty("resource") val resource: String,
    @JsonProperty("event") val event: String,
    @JsonProperty("event_time") val eventTime: Instant,
    @JsonProperty("payload") val payload: Map<String, Any>
) {
    fun toCommand(): CreateAuditEventCommand {
        return CreateAuditEventCommand(actorId, actorName, merchantId, identityType, resource, event, eventTime, payload)
    }
}

data class CreateAuditEventResponse(
    val success: Boolean,
    val message: String? = null
)