package ai.sovereignrag.identity.core.registration.controller

import ai.sovereignrag.identity.core.registration.command.RegisterUserCommand
import ai.sovereignrag.identity.core.registration.command.ResendVerificationCommand
import ai.sovereignrag.identity.core.registration.command.VerifyEmailCommand
import an.awesome.pipelinr.Pipeline
import com.giffing.bucket4j.spring.boot.starter.context.RateLimiting
import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/registration")
@Tag(name = "Registration", description = "User registration and email verification endpoints")
class RegistrationController(
    private val pipeline: Pipeline
) {

    @PostMapping("/register")
    @RateLimiting(
        name = "user-invitation",
        cacheKey = "#request.email"
    )
    @Operation(summary = "Register new user", description = "Register a new user with business email")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "User registered successfully",
            content = [Content(mediaType = "application/json",
                schema = Schema(implementation = RegisterUserResponse::class))]),
        ApiResponse(responseCode = "400", description = "Invalid email or user already exists")
    ])
    fun registerUser(
        @RequestBody request: RegisterUserRequest
    ): RegisterUserResponse {
        log.info { "Received registration request for email: ${request.email}" }

        val result = pipeline.send(
            RegisterUserCommand(
                email = request.email,
                password = request.password,
                fullName = request.fullName,
                organizationName = request.organizationName
            )
        )

        return RegisterUserResponse(
            success = result.success,
            message = result.message,
            userId = result.userId,
            organizationId = result.organizationId,
            isNewOrganization = result.isNewOrganization
        )
    }

    @PostMapping("/verify-email")
    @RateLimiting(
        name = "invitation-validate",
        cacheKey = "#request.token"
    )
    @Operation(summary = "Verify email", description = "Verify user email with verification token")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Email verified successfully",
            content = [Content(mediaType = "application/json",
                schema = Schema(implementation = VerifyEmailResponse::class))]),
        ApiResponse(responseCode = "400", description = "Invalid or expired token")
    ])
    fun verifyEmail(
        @RequestBody request: VerifyEmailRequest
    ): VerifyEmailResponse {
        log.info { "Received email verification request" }

        val result = pipeline.send(
            VerifyEmailCommand(token = request.token)
        )

        return VerifyEmailResponse(
            success = result.success,
            message = result.message,
            userId = result.userId
        )
    }

    @PostMapping("/resend-verification")
    @RateLimiting(
        name = "resend-invitation",
        cacheKey = "#request.email"
    )
    @Operation(summary = "Resend verification email", description = "Resend email verification to unverified user")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Verification email sent",
            content = [Content(mediaType = "application/json",
                schema = Schema(implementation = ResendVerificationResponse::class))]),
        ApiResponse(responseCode = "400", description = "User not found or already verified")
    ])
    fun resendVerification(
        @RequestBody request: ResendVerificationRequest
    ): ResendVerificationResponse {
        log.info { "Received request to resend verification for email: ${request.email}" }

        val result = pipeline.send(
            ResendVerificationCommand(email = request.email)
        )

        return ResendVerificationResponse(
            success = result.success,
            message = result.message
        )
    }
}
