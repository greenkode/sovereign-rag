package ai.sovereignrag.identity.core.profile.command

import ai.sovereignrag.commons.user.dto.PhoneNumber
import an.awesome.pipelinr.Command

data class UpdateProfileCommand(
    val firstName: String?,
    val lastName: String?,
    val phoneNumber: PhoneNumber?,
    val locale: String?
) : Command<UpdateProfileResult>

data class UpdateProfileResult(
    val success: Boolean,
    val message: String
)
