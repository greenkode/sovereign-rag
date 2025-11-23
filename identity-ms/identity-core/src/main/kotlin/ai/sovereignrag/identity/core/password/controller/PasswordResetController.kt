package ai.sovereignrag.identity.core.password.controller

import ai.sovereignrag.identity.core.integration.CompletePasswordResetRequest
import ai.sovereignrag.identity.core.integration.InitiatePasswordResetRequest
import ai.sovereignrag.identity.core.integration.ValidatePasswordResetRequest
import ai.sovereignrag.identity.core.integration.ValidatePasswordResetResponse
import ai.sovereignrag.identity.core.password.command.CompletePasswordResetCommand
import ai.sovereignrag.identity.core.password.command.CompletePasswordResetResult
import ai.sovereignrag.identity.core.password.command.InitiatePasswordResetCommand
import ai.sovereignrag.identity.core.password.command.InitiatePasswordResetResult
import ai.sovereignrag.identity.core.password.command.ValidatePasswordResetCommand
import an.awesome.pipelinr.Pipeline
import com.giffing.bucket4j.spring.boot.starter.context.RateLimiting
import mu.KotlinLogging
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
@RequestMapping("/password-reset")
@Tag(name = "Password Reset", description = "Password reset and recovery endpoints")
class PasswordResetController(
    private val pipeline: Pipeline
) {

    @PostMapping("/initiate")
    @RateLimiting(
        name = "password-reset",
        cacheKey = "#request.email"
    )
    @Operation(summary = "Initiate password reset", description = "Start password reset process by sending reset email")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Password reset initiated",
            content = [Content(mediaType = "application/json",
                schema = Schema(implementation = InitiatePasswordResetResult::class))]),
        ApiResponse(responseCode = "404", description = "Email not found")
    ])
    fun initiatePasswordReset(
        @RequestBody request: InitiatePasswordResetRequest
    ): InitiatePasswordResetResult {
        log.info { "Received request to initiate password reset for email: ${request.email}" }

        return pipeline.send(InitiatePasswordResetCommand(request.email))
    }

    @PostMapping("/validate")
    @RateLimiting(
        name = "password-reset",
        cacheKey = "#request.token"
    )
    @Operation(summary = "Validate reset token", description = "Validate password reset token")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Token validation result",
            content = [Content(mediaType = "application/json",
                schema = Schema(implementation = ValidatePasswordResetResponse::class))]),
        ApiResponse(responseCode = "400", description = "Invalid or expired token")
    ])
    fun validatePasswordReset(
        @RequestBody request: ValidatePasswordResetRequest
    ): ValidatePasswordResetResponse {
        log.info { "Received request to validate password reset" }

        val result = pipeline.send(
            ValidatePasswordResetCommand(
                token = request.token
            )
        )

        return ValidatePasswordResetResponse(
            result.success, result.reference, result.userId, result.message
        )
    }

    @PostMapping("/complete")
    @RateLimiting(
        name = "password-reset",
        cacheKey = "#request.reference"
    )
    @Operation(summary = "Complete password reset", description = "Complete password reset with new password")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Password reset completed",
            content = [Content(mediaType = "application/json",
                schema = Schema(implementation = CompletePasswordResetResult::class))]),
        ApiResponse(responseCode = "400", description = "Invalid token or reference")
    ])
    fun completePasswordReset(
        @RequestBody request: CompletePasswordResetRequest
    ): CompletePasswordResetResult {
        log.info { "Received request to complete password reset" }

        return pipeline.send(
            CompletePasswordResetCommand(
                reference = request.reference,
                token = request.token,
                newPassword = request.newPassword
            )
        )
    }
}