package ai.sovereignrag.identity.core.profile.controller

import ai.sovereignrag.commons.user.dto.PhoneNumber
import ai.sovereignrag.identity.core.profile.command.GenerateAvatarCommand
import ai.sovereignrag.identity.core.profile.command.UpdateProfileCommand
import ai.sovereignrag.identity.core.profile.command.UploadAvatarCommand
import ai.sovereignrag.identity.core.profile.dto.GenerateAvatarRequest
import ai.sovereignrag.identity.core.profile.dto.GenerateAvatarResponse
import ai.sovereignrag.identity.core.profile.dto.UpdateProfileRequest
import ai.sovereignrag.identity.core.profile.dto.UpdateProfileResponse
import ai.sovereignrag.identity.core.profile.dto.UploadAvatarResponse
import ai.sovereignrag.identity.core.profile.dto.UserProfileResponse
import ai.sovereignrag.identity.core.profile.query.GetUserProfileQuery
import an.awesome.pipelinr.Pipeline
import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.Locale
import java.util.UUID

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/profile")
@Tag(name = "Profile", description = "User profile management")
class ProfileController(
    private val pipeline: Pipeline
) {

    @GetMapping
    @Operation(summary = "Get user profile", description = "Retrieve current user's profile information")
    @ApiResponse(
        responseCode = "200", description = "Profile retrieved",
        content = [Content(
            mediaType = "application/json",
            schema = Schema(implementation = UserProfileResponse::class)
        )]
    )
    @SecurityRequirement(name = "bearerAuth")
    fun getProfile(): UserProfileResponse {
        log.info { "Getting user profile" }

        val result = pipeline.send(GetUserProfileQuery())

        return UserProfileResponse(
            id = result.id,
            username = result.username,
            email = result.email,
            firstName = result.firstName,
            lastName = result.lastName,
            phoneNumber = result.phoneNumber,
            pictureUrl = result.pictureUrl,
            locale = result.locale,
            emailVerified = result.emailVerified
        )
    }

    @PutMapping
    @Operation(summary = "Update user profile", description = "Update current user's profile information")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200", description = "Profile updated",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = UpdateProfileResponse::class)
                )]
            ),
            ApiResponse(responseCode = "400", description = "Invalid request data")
        ]
    )
    @SecurityRequirement(name = "bearerAuth")
    fun updateProfile(@RequestBody request: UpdateProfileRequest): UpdateProfileResponse {
        log.info { "Updating user profile" }

        val phoneNumber = request.phoneNumber?.takeIf { it.isNotBlank() }?.let { number ->
            request.countryCode?.takeIf { it.isNotBlank() }?.let { code ->
                val locale = Locale.Builder().setRegion(code).build()
                PhoneNumber(number, locale)
            }
        }

        val result = pipeline.send(
            UpdateProfileCommand(
                firstName = request.firstName,
                lastName = request.lastName,
                phoneNumber = phoneNumber,
                locale = request.locale
            )
        )

        return UpdateProfileResponse(
            success = result.success,
            message = result.message
        )
    }

    @PostMapping("/avatar", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(summary = "Upload avatar", description = "Upload a new profile picture")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200", description = "Avatar uploaded",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = UploadAvatarResponse::class)
                )]
            ),
            ApiResponse(responseCode = "400", description = "Invalid file or file too large")
        ]
    )
    @SecurityRequirement(name = "bearerAuth")
    fun uploadAvatar(@RequestParam("file") file: MultipartFile): UploadAvatarResponse {
        log.info { "Uploading avatar: ${file.originalFilename}, size: ${file.size}" }

        val result = pipeline.send(
            UploadAvatarCommand(
                inputStream = file.inputStream,
                fileName = file.originalFilename ?: "avatar",
                contentType = file.contentType ?: "image/png",
                size = file.size
            )
        )

        return UploadAvatarResponse(
            success = result.success,
            message = result.message,
            pictureUrl = result.pictureUrl
        )
    }

    @PostMapping("/avatar/generate")
    @Operation(summary = "Generate avatar", description = "Generate an avatar using AI or external services")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200", description = "Avatar generated",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = GenerateAvatarResponse::class)
                )]
            )
        ]
    )
    @SecurityRequirement(name = "bearerAuth")
    fun generateAvatar(@RequestBody request: GenerateAvatarRequest): GenerateAvatarResponse {
        log.info { "Generating avatar with style: ${request.style}, processId: ${request.processId}" }

        val processId = request.processId?.takeIf { it.isNotBlank() }?.let {
            runCatching { UUID.fromString(it) }.getOrNull()
        }

        val result = pipeline.send(
            GenerateAvatarCommand(
                style = request.style,
                backgroundColor = request.backgroundColor,
                prompt = request.prompt,
                processId = processId
            )
        )

        return GenerateAvatarResponse(
            success = result.success,
            message = result.message,
            pictureUrl = result.pictureUrl,
            processId = result.processId?.toString(),
            imageKey = result.imageKey
        )
    }
}
