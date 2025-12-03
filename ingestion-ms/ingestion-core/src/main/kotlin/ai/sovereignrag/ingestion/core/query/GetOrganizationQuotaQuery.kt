package ai.sovereignrag.ingestion.core.query

import ai.sovereignrag.ingestion.commons.dto.OrganizationQuotaResponse
import an.awesome.pipelinr.Command
import java.util.UUID

data class GetOrganizationQuotaQuery(
    val organizationId: UUID
) : Command<OrganizationQuotaResponse>
