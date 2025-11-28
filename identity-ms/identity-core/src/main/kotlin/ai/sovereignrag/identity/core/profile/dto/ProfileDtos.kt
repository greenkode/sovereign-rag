package ai.sovereignrag.identity.core.profile.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "User profile response")
data class UserProfileResponse(
    @Schema(description = "User ID")
    val id: String,
    @Schema(description = "Username")
    val username: String,
    @Schema(description = "Email address")
    val email: String,
    @Schema(description = "First name")
    val firstName: String?,
    @Schema(description = "Last name")
    val lastName: String?,
    @Schema(description = "Phone number")
    val phoneNumber: String?,
    @Schema(description = "Profile picture URL")
    val pictureUrl: String?,
    @Schema(description = "Locale preference")
    val locale: String,
    @Schema(description = "Whether email is verified")
    val emailVerified: Boolean
)

@Schema(description = "Update profile request")
data class UpdateProfileRequest(
    @Schema(description = "First name")
    val firstName: String?,
    @Schema(description = "Last name")
    val lastName: String?,
    @Schema(description = "Phone number")
    val phoneNumber: String?,
    @Schema(description = "Locale preference")
    val locale: String?
)

@Schema(description = "Update profile response")
data class UpdateProfileResponse(
    @Schema(description = "Whether update was successful")
    val success: Boolean,
    @Schema(description = "Response message")
    val message: String
)

@Schema(description = "Upload avatar response")
data class UploadAvatarResponse(
    @Schema(description = "Whether upload was successful")
    val success: Boolean,
    @Schema(description = "Response message")
    val message: String,
    @Schema(description = "URL of the uploaded avatar")
    val pictureUrl: String?
)

@Schema(description = "Generate avatar request")
data class GenerateAvatarRequest(
    @Schema(description = "Avatar style", example = "initials")
    val style: AvatarStyle = AvatarStyle.INITIALS,
    @Schema(description = "Background color (hex)", example = "#6366f1")
    val backgroundColor: String? = null
)

@Schema(description = "Generate avatar response")
data class GenerateAvatarResponse(
    @Schema(description = "Whether generation was successful")
    val success: Boolean,
    @Schema(description = "Response message")
    val message: String,
    @Schema(description = "URL of the generated avatar")
    val pictureUrl: String?
)

enum class AvatarStyle {
    INITIALS,
    DICEBEAR_AVATAAARS,
    DICEBEAR_BOTTTS,
    DICEBEAR_IDENTICON,
    DICEBEAR_SHAPES
}
