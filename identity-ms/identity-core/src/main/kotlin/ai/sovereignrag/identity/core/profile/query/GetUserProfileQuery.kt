package ai.sovereignrag.identity.core.profile.query

import an.awesome.pipelinr.Command

class GetUserProfileQuery : Command<GetUserProfileResult>

data class GetUserProfileResult(
    val id: String,
    val username: String,
    val email: String,
    val firstName: String?,
    val lastName: String?,
    val phoneNumber: String?,
    val pictureUrl: String?,
    val locale: String,
    val emailVerified: Boolean
)
