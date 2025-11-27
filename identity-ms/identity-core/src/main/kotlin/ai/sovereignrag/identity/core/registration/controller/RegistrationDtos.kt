package ai.sovereignrag.identity.core.registration.controller

import java.util.UUID

data class RegisterUserRequest(
    val email: String,
    val password: String,
    val fullName: String,
    val organizationName: String? = null
)

data class RegisterUserResponse(
    val success: Boolean,
    val message: String,
    val userId: UUID? = null,
    val organizationId: UUID? = null,
    val isNewOrganization: Boolean = false
)

data class VerifyEmailRequest(
    val token: String
)

data class VerifyEmailResponse(
    val success: Boolean,
    val message: String,
    val userId: UUID? = null
)

data class ResendVerificationRequest(
    val email: String
)

data class ResendVerificationResponse(
    val success: Boolean,
    val message: String
)
