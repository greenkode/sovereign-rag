package ai.sovereignrag.identity.core.settings.query

import ai.sovereignrag.commons.subscription.SubscriptionTier
import ai.sovereignrag.identity.core.entity.CompanyRole
import ai.sovereignrag.identity.core.entity.CompanySize
import ai.sovereignrag.identity.core.entity.EnvironmentMode
import ai.sovereignrag.identity.core.entity.IntendedPurpose
import ai.sovereignrag.identity.core.entity.OrganizationStatus
import an.awesome.pipelinr.Command

class GetOrganizationDetailsQuery : Command<GetOrganizationDetailsResult>

data class GetOrganizationDetailsResult(
    val id: String,
    val name: String,
    val plan: SubscriptionTier,
    val status: OrganizationStatus,
    val environmentMode: EnvironmentMode,
    val setupCompleted: Boolean,
    val intendedPurpose: IntendedPurpose?,
    val companySize: CompanySize?,
    val roleInCompany: CompanyRole?,
    val country: String?,
    val phoneNumber: String?,
    val website: String?,
    val email: String?
)
