package ai.sovereignrag.identity.core.settings.command

import ai.sovereignrag.identity.core.entity.CompanyRole
import ai.sovereignrag.identity.core.entity.CompanySize
import ai.sovereignrag.identity.core.entity.IntendedPurpose
import an.awesome.pipelinr.Command

data class CompleteOrganizationSetupCommand(
    val companyName: String,
    val intendedPurpose: IntendedPurpose,
    val companySize: CompanySize,
    val roleInCompany: CompanyRole,
    val country: String,
    val phoneNumber: String,
    val website: String?,
    val termsAccepted: Boolean
) : Command<CompleteOrganizationSetupResult>

data class CompleteOrganizationSetupResult(
    val success: Boolean,
    val message: String,
    val merchantId: String? = null
)
