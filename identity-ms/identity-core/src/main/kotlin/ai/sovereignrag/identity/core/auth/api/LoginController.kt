package ai.sovereignrag.identity.core.auth.api

import ai.sovereignrag.identity.commons.dto.TokenResponse
import ai.sovereignrag.identity.commons.dto.UserSummary
import ai.sovereignrag.identity.core.auth.command.LoginCommand
import ai.sovereignrag.identity.core.auth.command.LoginResult
import ai.sovereignrag.identity.core.service.ClientIpExtractionService
import an.awesome.pipelinr.Pipeline
import com.giffing.bucket4j.spring.boot.starter.context.RateLimiting
import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

@RestController
@Profile("local")
class LoginController(
    private val pipeline: Pipeline,
    private val clientIpExtractionService: ClientIpExtractionService
) {

    @PostMapping("/api/login")
    @RateLimiting(name = "login-attempts", cacheKey = "#loginRequest.username")
    @Operation(summary = "User login", description = "Authenticate user with username and password")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Login successful",
            content = [Content(mediaType = "application/json",
                schema = Schema(implementation = TokenResponse::class))]),
        ApiResponse(responseCode = "401", description = "Invalid credentials"),
        ApiResponse(responseCode = "423", description = "Account locked")
    ])
    fun apiLogin(@RequestBody loginRequest: LoginRequest): TokenResponse {
        log.info { "API login attempt for user: ${loginRequest.username}" }

        return pipeline.send(
            LoginCommand(
                username = loginRequest.username,
                password = loginRequest.password,
                ipAddress = clientIpExtractionService.getClientIpAddressFromContext()
            )
        ).toResponse()
    }

    private fun LoginResult.toResponse() = TokenResponse(
        accessToken = accessToken,
        refreshToken = refreshToken,
        expiresIn = expiresIn,
        user = UserSummary(
            username = username,
            name = fullName,
            email = email
        )
    )
}
