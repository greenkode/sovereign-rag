package ai.sovereignrag.identity.core.invitation.dto

import ai.sovereignrag.identity.commons.RoleEnum
import an.awesome.pipelinr.Command

data class CreateMerchantCommand(
    val clientName: String,
    val adminEmail: String
) : Command<CreateMerchantResult>

data class InviteUserCommand(
    val userEmail: String,
    val role: RoleEnum,
    val invitedByUserId: String
) : Command<InviteUserResult>

data class ResendInvitationCommand(
    val userEmail: String,
    val resendByUserId: String
) : Command<ResendInvitationResult>