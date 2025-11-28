package ai.sovereignrag.identity.core.settings.query

import ai.sovereignrag.identity.core.entity.EnvironmentMode
import ai.sovereignrag.identity.core.entity.OrganizationPlan
import ai.sovereignrag.identity.core.entity.OrganizationStatus
import an.awesome.pipelinr.Command

class GetOrganizationDetailsQuery : Command<GetOrganizationDetailsResult>

data class GetOrganizationDetailsResult(
    val id: String,
    val name: String,
    val plan: OrganizationPlan,
    val status: OrganizationStatus,
    val environmentMode: EnvironmentMode,
    val setupCompleted: Boolean
)
