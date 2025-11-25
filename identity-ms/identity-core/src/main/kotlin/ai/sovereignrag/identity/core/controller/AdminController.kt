package ai.sovereignrag.identity.core.controller

import ai.sovereignrag.identity.commons.dto.LockoutStatusResponse
import ai.sovereignrag.identity.commons.dto.UnlockResponse
import ai.sovereignrag.identity.commons.exception.NotFoundException
import ai.sovereignrag.identity.core.service.AccountLockoutService
import ai.sovereignrag.identity.core.service.ClientLockoutService
import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Administrative operations for user and client management")
class AdminController(
    private val accountLockoutService: AccountLockoutService,
    private val clientLockoutService: ClientLockoutService
) {

    @PostMapping("/unlock-user/{username}")
    @Operation(summary = "Unlock user account", description = "Unlock a locked user account")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "User unlocked successfully",
            content = [Content(mediaType = "application/json",
                schema = Schema(implementation = UnlockResponse::class))]),
        ApiResponse(responseCode = "404", description = "User not found or not locked")
    ])
    @SecurityRequirement(name = "bearerAuth")
    fun unlockUser(
        @Parameter(description = "Username to unlock", example = "john.doe", required = true)
        @PathVariable username: String
    ): UnlockResponse {
        log.info { "Admin request to unlock user: $username" }

        return accountLockoutService.unlockAccount(username)
            .takeIf { it }
            ?.let {
                log.info { "Successfully unlocked user: $username" }
                UnlockResponse.toResponse(success = true, identifier = username, isUser = true)
            }
            ?: throw NotFoundException("User not found or account was not locked: $username")
    }

    @PostMapping("/unlock-client/{clientId}")
    @Operation(summary = "Unlock client", description = "Unlock a locked OAuth client")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Client unlocked successfully",
            content = [Content(mediaType = "application/json",
                schema = Schema(implementation = UnlockResponse::class))]),
        ApiResponse(responseCode = "404", description = "Client not found or not locked")
    ])
    @SecurityRequirement(name = "bearerAuth")
    fun unlockClient(
        @Parameter(description = "Client ID to unlock", example = "client_123", required = true)
        @PathVariable clientId: String
    ): UnlockResponse {
        log.info { "Admin request to unlock client: $clientId" }

        return clientLockoutService.unlockClient(clientId)
            .takeIf { it }
            ?.let {
                log.info { "Successfully unlocked client: $clientId" }
                UnlockResponse.toResponse(success = true, identifier = clientId, isUser = false)
            }
            ?: throw NotFoundException("Client not found or was not locked: $clientId")
    }

    @GetMapping("/lockout-status/user/{username}")
    @Operation(summary = "Get user lockout status", description = "Check if a user account is locked")
    @ApiResponse(responseCode = "200", description = "Lockout status retrieved",
        content = [Content(mediaType = "application/json",
            schema = Schema(implementation = LockoutStatusResponse::class))])
    @SecurityRequirement(name = "bearerAuth")
    fun getUserLockoutStatus(
        @Parameter(description = "Username to check", example = "john.doe", required = true)
        @PathVariable username: String
    ): LockoutStatusResponse {
        log.info { "Admin checking lockout status for user: $username" }
        return LockoutStatusResponse.toResponse(
            identifier = username,
            remainingMinutes = accountLockoutService.getRemainingLockoutMinutes(username),
            isUser = true
        )
    }

    @GetMapping("/lockout-status/client/{clientId}")
    @Operation(summary = "Get client lockout status", description = "Check if an OAuth client is locked")
    @ApiResponse(responseCode = "200", description = "Client lockout status retrieved",
        content = [Content(mediaType = "application/json",
            schema = Schema(implementation = LockoutStatusResponse::class))])
    @SecurityRequirement(name = "bearerAuth")
    fun getClientLockoutStatus(
        @Parameter(description = "Client ID to check", example = "client_123", required = true)
        @PathVariable clientId: String
    ): LockoutStatusResponse {
        log.info { "Admin checking lockout status for client: $clientId" }
        return LockoutStatusResponse.toResponse(
            identifier = clientId,
            remainingMinutes = clientLockoutService.getRemainingLockoutMinutes(clientId),
            isUser = false
        )
    }
}
