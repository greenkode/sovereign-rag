package ai.sovereignrag.identity.core.settings.dto

import ai.sovereignrag.identity.core.entity.EnvironmentMode
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "Switch environment request")
data class SwitchEnvironmentRequest(
    @Schema(description = "Target environment to switch to", example = "SANDBOX", required = true)
    val environment: EnvironmentMode
)

@Schema(description = "Environment status and token response")
data class EnvironmentStatusResponse(
    @Schema(description = "Current active environment", example = "SANDBOX")
    val currentEnvironment: EnvironmentMode,
    @Schema(description = "User's environment preference", example = "SANDBOX")
    val environmentPreference: EnvironmentMode,
    @Schema(description = "Merchant's environment mode", example = "PRODUCTION")
    val merchantEnvironmentMode: EnvironmentMode,
    @Schema(description = "Last time environment was switched")
    val lastSwitchedAt: Instant?,
    @Schema(description = "Whether user can switch to production", example = "true")
    val canSwitchToProduction: Boolean,
    @Schema(description = "Whether re-authentication is required", example = "false")
    val requiresReAuthentication: Boolean = false,
    @Schema(description = "New access token with updated environment claims")
    val accessToken: String? = null,
    @Schema(description = "New refresh token")
    val refreshToken: String? = null,
    @Schema(description = "Token type", example = "Bearer")
    val tokenType: String? = null,
    @Schema(description = "Token expiry time in seconds", example = "600")
    val expiresIn: Long? = null
)
