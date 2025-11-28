package ai.sovereignrag.identity.core.settings.command

import an.awesome.pipelinr.Command

data class CompleteOrganizationSetupCommand(
    val companyName: String,
    val generativeAiGoal: String,
    val companySize: String,
    val roleInCompany: String,
    val country: String,
    val phoneNumber: String,
    val termsAccepted: Boolean
) : Command<CompleteOrganizationSetupResult>

data class CompleteOrganizationSetupResult(
    val success: Boolean,
    val message: String,
    val merchantId: String? = null
)
