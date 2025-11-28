package ai.sovereignrag.ingestion.core.query

import ai.sovereignrag.ingestion.commons.dto.TenantQuotaResponse
import an.awesome.pipelinr.Command
import java.util.UUID

data class GetTenantQuotaQuery(
    val tenantId: UUID
) : Command<TenantQuotaResponse>
