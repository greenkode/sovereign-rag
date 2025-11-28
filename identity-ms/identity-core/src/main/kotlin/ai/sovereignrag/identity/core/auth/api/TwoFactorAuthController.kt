package ai.sovereignrag.identity.core.auth.api

import ai.sovereignrag.identity.core.auth.command.InitiateTwoFactorCommand
import ai.sovereignrag.identity.core.auth.command.LogoutCommand
import ai.sovereignrag.identity.core.auth.command.RefreshTokenCommand
import ai.sovereignrag.identity.core.auth.command.ResendTwoFactorCommand
import ai.sovereignrag.identity.core.auth.command.VerifyTwoFactorCommand
import ai.sovereignrag.identity.core.service.ClientIpExtractionService
import an.awesome.pipelinr.Pipeline
import com.giffing.bucket4j.spring.boot.starter.context.RateLimiting
import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpHeaders
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}


@RestController
@RequestMapping("/api/2fa")
@Tag(name = "Two-Factor Authentication", description = "Two-factor authentication endpoints")
class TwoFactorAuthController(
    private val pipeline: Pipeline,
    private val clientIpExtractionService: ClientIpExtractionService
) {

    @PostMapping("/login")
    @Operation(summary = "Initiate 2FA login", description = "Start two-factor authentication process")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "2FA initiated successfully",
            content = [Content(mediaType = "application/json",
                schema = Schema(implementation = TwoFactorLoginResponse::class))]),
        ApiResponse(responseCode = "401", description = "Invalid credentials")
    ])
    fun loginWith2FA(
        @RequestBody request: TwoFactorLoginRequest,
        httpRequest: HttpServletRequest
    ): TwoFactorLoginResponse {
        log.info { "2FA login request for user: ${request.username}" }

        val result = pipeline.send(
            InitiateTwoFactorCommand(
                username = request.username,
                password = request.password,
                ipAddress = clientIpExtractionService.getClientIpAddress(httpRequest),
                userAgent = httpRequest.getHeader(HttpHeaders.USER_AGENT),
                httpRequest = httpRequest
            )
        )

        return TwoFactorLoginResponse(
            status = "DIRECT_LOGIN",
            sessionId = null,
            message = "Login successful from trusted device",
            accessToken = result.accessToken,
            refreshToken = result.refreshToken,
            expiresIn = result.expiresIn
        )
    }

    @PostMapping("/resend")
    @RateLimiting(
        name = "2fa-resend",
        cacheKey = "#request.sessionId"
    )
    @Operation(summary = "Resend 2FA code", description = "Resend two-factor authentication code")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Code resent successfully",
            content = [Content(mediaType = "application/json",
                schema = Schema(implementation = TwoFactorResendResponse::class))]),
        ApiResponse(responseCode = "400", description = "Invalid session")
    ])
    fun resend2FA(
        @RequestBody request: TwoFactorResendRequest,
        httpRequest: HttpServletRequest
    ): TwoFactorResendResponse {
        log.info { "2FA resend request for session: ${request.sessionId}" }

        val result = pipeline.send(
            ResendTwoFactorCommand(
                sessionId = request.sessionId,
                ipAddress = clientIpExtractionService.getClientIpAddress(httpRequest),
                userAgent = httpRequest.getHeader(HttpHeaders.USER_AGENT),
            )
        )

        return TwoFactorResendResponse(
            message = result.message,
            sessionId = result.sessionId
        )
    }

    @PostMapping("/verify")
    @RateLimiting(
        name = "2fa-verify",
        cacheKey = "#request.sessionId"
    )
    @Operation(summary = "Verify 2FA code", description = "Verify two-factor authentication code and complete login")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Verification successful",
            content = [Content(mediaType = "application/json",
                schema = Schema(implementation = TwoFactorVerifyResponse::class))]),
        ApiResponse(responseCode = "400", description = "Invalid code or session")
    ])
    fun verify2FA(@RequestBody request: TwoFactorVerifyRequest): TwoFactorVerifyResponse {
        log.info { "2FA verification request for session: ${request.sessionId}" }

        val result = pipeline.send(
            VerifyTwoFactorCommand(
                sessionId = request.sessionId,
                code = request.code
            )
        )

        return TwoFactorVerifyResponse(
            accessToken = result.accessToken,
            refreshToken = result.refreshToken,
            tokenType = result.tokenType,
            expiresIn = result.expiresIn,
            scope = result.scope,
            user = TwoFactorVerifyUserResponse(
                username = result.user.username,
                name = result.user.name,
                email = result.user.email
            ),
            message = result.message
        )
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Exchange refresh token for new access token")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Token refreshed successfully",
            content = [Content(mediaType = "application/json",
                schema = Schema(implementation = RefreshTokenResponse::class))]),
        ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    ])
    fun refreshToken(@RequestBody request: RefreshTokenRequest): RefreshTokenResponse {
        log.info { "Token refresh request received" }

        val result = pipeline.send(
            RefreshTokenCommand(
                refreshToken = request.refreshToken
            )
        )

        return RefreshTokenResponse(
            accessToken = result.accessToken,
            refreshToken = result.refreshToken,
            tokenType = result.tokenType,
            expiresIn = result.expiresIn,
            scope = result.scope
        )
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Logout user and revoke refresh token")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Logout successful",
            content = [Content(mediaType = "application/json",
                schema = Schema(implementation = LogoutResponse::class))])
    ])
    fun logout(@RequestBody request: LogoutRequest): LogoutResponse {
        log.info { "Logout request received" }

        val result = pipeline.send(
            LogoutCommand(
                refreshToken = request.refreshToken
            )
        )

        return LogoutResponse(
            success = result.success,
            message = result.message
        )
    }
}