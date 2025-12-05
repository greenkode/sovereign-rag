package ai.sovereignrag.identity.core.invitation.controller

import ai.sovereignrag.identity.commons.PhoneNumber
import ai.sovereignrag.identity.commons.RoleEnum
import ai.sovereignrag.commons.exception.RecordNotFoundException
import ai.sovereignrag.commons.security.IsMerchantSuperAdmin
import ai.sovereignrag.identity.core.invitation.command.CompleteInvitationCommand
import ai.sovereignrag.identity.core.invitation.command.CompleteInvitationResult
import ai.sovereignrag.identity.core.invitation.command.ValidateInvitationCommand
import ai.sovereignrag.identity.core.invitation.command.ValidateInvitationResult
import ai.sovereignrag.identity.core.invitation.dto.InviteUserCommand
import ai.sovereignrag.identity.core.invitation.dto.InviteUserRequest
import ai.sovereignrag.identity.core.invitation.dto.InviteUserResponse
import ai.sovereignrag.identity.core.invitation.dto.ResendInvitationCommand
import ai.sovereignrag.identity.core.invitation.dto.ResendInvitationRequest
import ai.sovereignrag.identity.core.invitation.dto.ResendInvitationResponse
import ai.sovereignrag.identity.core.invitation.dto.UserListResponse
import ai.sovereignrag.identity.core.invitation.query.GetUsersQuery
import ai.sovereignrag.identity.core.repository.OAuthUserRepository
import an.awesome.pipelinr.Pipeline
import com.giffing.bucket4j.spring.boot.starter.context.RateLimiting
import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.Locale
import java.util.UUID

@RestController
@RequestMapping("/merchant/invitation")
@Tag(name = "Merchant Invitations", description = "Merchant user invitation management")
class MerchantInvitationController(
    private val pipeline: Pipeline,
    private val userRepository: OAuthUserRepository
) {

    private val log = KotlinLogging.logger {}

    @PostMapping("/validate")
    @RateLimiting(
        name = "invitation-validate",
        cacheKey = "#token"
    )
    @Operation(summary = "Validate invitation token", description = "Validate merchant invitation token")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Token validation result",
            content = [Content(mediaType = "application/json",
                schema = Schema(implementation = ValidateInvitationResult::class))]),
        ApiResponse(responseCode = "400", description = "Invalid token")
    ])
    fun validateInvitation(
        @Parameter(description = "Invitation token", example = "inv_token_123", required = true)
        @RequestParam token: String
    ): ValidateInvitationResult {
        log.info { "Received request to validate merchant invitation $token" }

        return pipeline.send(ValidateInvitationCommand(token))
    }

    @PostMapping("/complete")
    @RateLimiting(
        name = "invitation-complete",
        cacheKey = "#request.reference"
    )
    @Operation(summary = "Complete invitation", description = "Complete merchant invitation by setting up user account")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Invitation completed successfully",
            content = [Content(mediaType = "application/json",
                schema = Schema(implementation = CompleteInvitationResult::class))]),
        ApiResponse(responseCode = "400", description = "Invalid invitation data")
    ])
    fun completeInvitation(@RequestBody request: CompleteInvitationRequest): CompleteInvitationResult {
        log.info { "Received request to complete merchant invitation" }

        return pipeline.send(
            CompleteInvitationCommand(
                token = request.token,
                reference = request.reference,
                fullName = request.fullName,
                phoneNumber = PhoneNumber(request.phoneNumber, Locale.of("en", "NG")),
                password = request.password
            )
        )
    }

    @PostMapping("/invite")
    @IsMerchantSuperAdmin
    @ResponseStatus(HttpStatus.CREATED)
    @RateLimiting(
        name = "user-invitation",
        cacheKey = "#request.userEmail"
    )
    @Operation(summary = "Invite user", description = "Send invitation to new user")
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "Invitation sent successfully",
            content = [Content(mediaType = "application/json",
                schema = Schema(implementation = InviteUserResponse::class))]),
        ApiResponse(responseCode = "400", description = "Invalid request data"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions")
    ])
    @SecurityRequirement(name = "bearerAuth")
    fun inviteUser(
        @RequestBody request: InviteUserRequest,
        @AuthenticationPrincipal jwt: Jwt
    ): InviteUserResponse {
        log.info { "Received request to invite user: ${request.userEmail}" }

        val currentUsername = jwt.subject
            ?: throw RecordNotFoundException("Invalid authentication token")

        val currentUser = userRepository.findById(UUID.fromString(currentUsername))
            .orElseThrow { RecordNotFoundException("User not found") }

        val result = pipeline.send(
            InviteUserCommand(
                userEmail = request.userEmail,
                role = request.role,
                invitedByUserId = currentUser.id.toString()
            )
        )

        return InviteUserResponse(
            success = result.success,
            message = result.message,
            invitationId = result.invitationId
        )
    }

    @PostMapping("/resend")
    @IsMerchantSuperAdmin
    @RateLimiting(
        name = "resend-invitation",
        cacheKey = "#request.userEmail"
    )
    @Operation(summary = "Resend invitation", description = "Resend invitation to user")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Invitation resent successfully",
            content = [Content(mediaType = "application/json",
                schema = Schema(implementation = ResendInvitationResponse::class))]),
        ApiResponse(responseCode = "404", description = "Invitation not found"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions")
    ])
    @SecurityRequirement(name = "bearerAuth")
    fun resendInvitation(
        @RequestBody request: ResendInvitationRequest,
        @AuthenticationPrincipal jwt: Jwt
    ): ResendInvitationResponse {
        log.info { "Received request to resend invitation to: ${request.userEmail}" }

        val currentUsername = jwt.subject
            ?: throw RecordNotFoundException("Invalid authentication token")

        val currentUser = userRepository.findById(UUID.fromString(currentUsername))
            .orElseThrow { RecordNotFoundException("User not found") }

        val result = pipeline.send(
            ResendInvitationCommand(
                userEmail = request.userEmail,
                resendByUserId = currentUser.id.toString()
            )
        )

        return ResendInvitationResponse(
            success = result.success,
            message = result.message
        )
    }

    @IsMerchantSuperAdmin
    @GetMapping("/users")
    @Operation(summary = "Get merchant users", description = "Get list of users for the merchant")
    @ApiResponse(responseCode = "200", description = "Users retrieved successfully",
        content = [Content(mediaType = "application/json",
            schema = Schema(implementation = UserListResponse::class))])
    @SecurityRequirement(name = "bearerAuth")
    fun getUsers(): UserListResponse {
        log.info { "Fetching users for merchant" }

        val result = pipeline.send(GetUsersQuery())
        return UserListResponse(result.users)
    }

    @IsMerchantSuperAdmin
    @GetMapping("/roles")
    @Operation(summary = "Get available roles", description = "Get list of available roles for user invitation")
    @ApiResponse(responseCode = "200", description = "Roles retrieved successfully",
        content = [Content(mediaType = "application/json",
            schema = Schema(type = "array", implementation = RoleResponse::class))])
    @SecurityRequirement(name = "bearerAuth")
    fun getAvailableRoles(): List<RoleResponse> {
        log.info { "Fetching available roles for user invitation" }

        return listOf(RoleResponse(RoleEnum.ROLE_MERCHANT_USER.name, RoleEnum.ROLE_MERCHANT_USER.description),
            RoleResponse(RoleEnum.ROLE_MERCHANT_ADMIN.name, RoleEnum.ROLE_MERCHANT_ADMIN.description),)
    }
}
