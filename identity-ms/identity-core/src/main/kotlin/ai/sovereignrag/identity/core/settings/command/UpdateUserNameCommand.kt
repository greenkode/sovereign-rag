package ai.sovereignrag.identity.core.settings.command

import an.awesome.pipelinr.Command

data class UpdateUserNameCommand(
    val firstName: String?,
    val lastName: String?
) : Command<UpdateUserNameResult>

data class UpdateUserNameResult(
    val success: Boolean,
    val message: String
)