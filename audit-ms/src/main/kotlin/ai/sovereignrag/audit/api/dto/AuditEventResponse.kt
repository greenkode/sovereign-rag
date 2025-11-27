package ai.sovereignrag.audit.api.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

class AuditEventResponse(
    @JsonProperty("actor_id") val actorId: String,
    @JsonProperty("actor_name") val actorName: String,
    @JsonProperty("merchant_id") val merchantId: String,
    @JsonProperty("identity_type") val identityType: String,
    @JsonProperty("resource") val resource: String,
    @JsonProperty("event_type") val eventType: String,
    @JsonProperty("event_time") val eventTime: LocalDateTime,
    @JsonProperty("time_recorded") val timeRecorded: LocalDateTime,
    @JsonProperty("payload") val payload: Map<String, Any> = mapOf(),
    @JsonProperty("ip_address") val ipAddress: String = "N/A"
)
