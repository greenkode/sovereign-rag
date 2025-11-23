package ai.sovereignrag.identity.core.invitation.controller

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Complete invitation request")
data class CompleteInvitationRequest(
    @Schema(description = "Invitation token", example = "inv_token_123", required = true)
    val token: String,
    @Schema(description = "Invitation reference", example = "ref_456", required = true)
    val reference: String,
    @Schema(description = "User full name", example = "John Doe", required = true)
    val fullName: String,
    @Schema(description = "Phone number", example = "+2348012345678", required = true)
    val phoneNumber: String,
    @Schema(description = "User password", example = "securePassword123", required = true)
    val password: String
)