package ai.sovereignrag.identity.core.invitation.dto

import ai.sovereignrag.identity.commons.RoleEnum
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

data class MerchantDto(
    val id: String,
    val clientId: String,
    val clientName: String,
    val clientAuthenticationMethods: List<String>,
    val authorizationGrantTypes: List<String>,
    val redirectUris: List<String>?,
    val postLogoutRedirectUris: List<String>?,
    val scopes: List<String>,
    val requireAuthorizationConsent: Boolean,
    val requireProofKey: Boolean,
    val accessTokenTimeToLive: String,
    val refreshTokenTimeToLive: String,
    val reuseRefreshTokens: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
    val isLocked: Boolean,
    val failedAuthAttempts: Int
)

@Schema(description = "Create merchant request")
data class CreateMerchantRequest(
    @Schema(description = "Merchant client name", example = "ABC Company Ltd", required = true)
    val clientName: String,
    @Schema(description = "Admin email address", example = "admin@company.com", required = true)
    val adminEmail: String,
)

data class CreateMerchantResult(
    val success: Boolean,
    val message: String,
    val id: String
)

@Schema(description = "Create merchant response")
data class CreateMerchantResponse(
    @Schema(description = "Whether creation was successful", example = "true")
    val success: Boolean,
    @Schema(description = "Response message", example = "Merchant created successfully")
    val message: String,
    @Schema(description = "Created merchant ID", example = "merchant_123")
    val id: String
)

@Schema(description = "Invite user request")
data class InviteUserRequest(
    @Schema(description = "Email address of user to invite", example = "user@example.com", required = true)
    val userEmail: String,
    @Schema(description = "Role to assign to user", example = "MERCHANT_ADMIN", required = true)
    val role: RoleEnum,
)

data class InviteUserResult(
    val success: Boolean,
    val message: String,
    val invitationId: String? = null
)

@Schema(description = "Invite user response")
data class InviteUserResponse(
    @Schema(description = "Whether invitation was successful", example = "true")
    val success: Boolean,
    @Schema(description = "Response message", example = "Invitation sent successfully")
    val message: String,
    @Schema(description = "Invitation ID", example = "inv_123456")
    val invitationId: String? = null
)

@Schema(description = "Resend invitation request")
data class ResendInvitationRequest(
    @Schema(description = "Email address of user", example = "user@example.com", required = true)
    val userEmail: String
)

data class ResendInvitationResult(
    val success: Boolean,
    val message: String
)

@Schema(description = "Resend invitation response")
data class ResendInvitationResponse(
    @Schema(description = "Whether resend was successful", example = "true")
    val success: Boolean,
    @Schema(description = "Response message", example = "Invitation resent successfully")
    val message: String
)

@Schema(description = "User list item")
data class UserListItem(
    @Schema(description = "User ID", example = "user_123")
    val id: String,
    @Schema(description = "Username", example = "john.doe")
    val username: String,
    @Schema(description = "Full name", example = "John Doe")
    val fullName: String,
    @Schema(description = "User role", example = "MERCHANT_ADMIN")
    val role: String,
    @Schema(description = "User status", example = "ACTIVE")
    val status: UserStatus,
    @Schema(description = "User creation date", example = "2023-01-01T10:00:00Z")
    val createdAt: Instant
)

@Schema(description = "User status enumeration")
enum class UserStatus {
    @Schema(description = "Active user") ACTIVE,
    @Schema(description = "Pending invitation") PENDING
}

@Schema(description = "User list response")
data class UserListResponse(
    @Schema(description = "List of users")
    val users: List<UserListItem>
)