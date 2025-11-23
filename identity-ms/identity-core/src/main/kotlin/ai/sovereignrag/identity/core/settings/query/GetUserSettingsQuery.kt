package ai.sovereignrag.identity.core.settings.query

import an.awesome.pipelinr.Command

class GetUserSettingsQuery() : Command<GetUserSettingsResult>

data class GetUserSettingsResult(
    val firstName: String?,
    val lastName: String?,
    val email: String,
    val roles: List<String>,
    val merchantName: String?
)