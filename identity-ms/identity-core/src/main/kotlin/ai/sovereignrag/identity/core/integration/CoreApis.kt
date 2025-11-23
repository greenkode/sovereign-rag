package ai.sovereignrag.identity.core.integration

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(description = "Password reset initiation request")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder(
    "email",
)
data class InitiatePasswordResetRequest(
    @Schema(description = "Email address of the user", example = "user@example.com", required = true)
    @JsonProperty("email")
    val email: String,
)

@Schema(description = "Password reset token validation request")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder(
    "reference",
    "token"
)
data class ValidatePasswordResetRequest(
    @Schema(description = "Password reset token", example = "reset_token_123", required = true)
    @JsonProperty("token")
    val token: String
)
@Schema(description = "Complete password reset request")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder(
    "reference",
    "token"
)
data class CompletePasswordResetRequest(
    @Schema(description = "Password reset reference", example = "ref_123456", required = true)
    @JsonProperty("reference")
    val reference: String,

    @Schema(description = "Password reset token", example = "reset_token_123", required = true)
    @JsonProperty("token")
    val token: String,

    @Schema(description = "New password", example = "newPassword123", required = true)
    @JsonProperty("new_password")
    val newPassword: String
)

@Schema(description = "Password reset token validation response")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder(
    "success",
    "reference",
    "user_id",
    "message"
)
data class ValidatePasswordResetResponse(
    @Schema(description = "Whether validation was successful", example = "true")
    @JsonProperty("success")
    val success: Boolean,

    @Schema(description = "Reset reference UUID", example = "550e8400-e29b-41d4-a716-446655440000")
    @JsonProperty("reference")
    val reference: UUID,

    @Schema(description = "User UUID", example = "550e8400-e29b-41d4-a716-446655440001")
    @JsonProperty("user_id")
    val userId: UUID,

    @Schema(description = "Response message", example = "Token is valid")
    @JsonProperty("message")
    val message: String
)