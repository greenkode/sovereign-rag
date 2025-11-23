package ai.sovereignrag.audit.api.dto

import ai.sovereignrag.audit.domain.query.GetAuditLogsByMerchantQuery
import org.springframework.data.domain.Sort.Direction

data class AuditEventByMerchantRequest(
    val merchantId: String,
    override val page: Int?,
    override val size: Int?,
    override val sort: Direction?,
    val actorId: String? = null,
    val identityType: String? = null,
    val eventType: String? = null
) : PageRequestDto(page, size, sort) {

    fun toDomainQuery(): GetAuditLogsByMerchantQuery {
        return GetAuditLogsByMerchantQuery(
            merchantId,
            toDomain()
        )
    }
}