package ai.sovereignrag.identity.core.invitation.command

import an.awesome.pipelinr.Command

data class ValidateInvitationCommand(
    val token: String
) : Command<ValidateInvitationResult>

data class ValidateInvitationResult(
    val success: Boolean,
    val token: String,
    val authReference: String
)