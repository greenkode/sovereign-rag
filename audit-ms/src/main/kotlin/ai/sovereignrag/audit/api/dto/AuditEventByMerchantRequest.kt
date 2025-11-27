package ai.sovereignrag.audit.api.dto

import ai.sovereignrag.audit.domain.query.GetAuditLogsByMerchantQuery
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.data.domain.Sort.Direction

data class AuditEventByMerchantRequest(
    @JsonProperty("merchant_id") val merchantId: String,
    @JsonProperty("page") override val page: Int?,
    @JsonProperty("size") override val size: Int?,
    @JsonProperty("sort") override val sort: Direction?,
    @JsonProperty("actor_id") val actorId: String? = null,
    @JsonProperty("identity_type") val identityType: String? = null,
    @JsonProperty("event_type") val eventType: String? = null
) : PageRequestDto(page, size, sort) {

    fun toDomainQuery(): GetAuditLogsByMerchantQuery {
        return GetAuditLogsByMerchantQuery(
            merchantId,
            toDomain()
        )
    }
}