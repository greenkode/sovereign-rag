package ai.sovereignrag.audit.domain.query

import ai.sovereignrag.audit.domain.model.AuditLog
import ai.sovereignrag.audit.domain.model.PageRequest
import an.awesome.pipelinr.Command
import org.springframework.data.domain.Page

data class GetAuditLogsByMerchantQuery(
    val merchantId: String,
    val page: PageRequest,
) : Command<Page<AuditLog>>
