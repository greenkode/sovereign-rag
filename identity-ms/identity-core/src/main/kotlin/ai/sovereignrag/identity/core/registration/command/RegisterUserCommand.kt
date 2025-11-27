package ai.sovereignrag.identity.core.registration.command

import an.awesome.pipelinr.Command
import java.util.UUID

data class RegisterUserCommand(
    val email: String,
    val password: String,
    val fullName: String,
    val organizationName: String? = null
) : Command<RegisterUserResult>

data class RegisterUserResult(
    val success: Boolean,
    val message: String,
    val userId: UUID? = null,
    val organizationId: UUID? = null,
    val isNewOrganization: Boolean = false,
    val verificationRequired: Boolean = true
)
