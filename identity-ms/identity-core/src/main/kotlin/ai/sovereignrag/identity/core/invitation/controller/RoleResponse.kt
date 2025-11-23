package ai.sovereignrag.identity.core.invitation.controller

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Role information for user invitation")
data class RoleResponse(
    @Schema(description = "Role name", example = "MERCHANT_ADMIN")
    val name: String,
    @Schema(description = "Role description", example = "Merchant administrator with full access")
    val description: String
)