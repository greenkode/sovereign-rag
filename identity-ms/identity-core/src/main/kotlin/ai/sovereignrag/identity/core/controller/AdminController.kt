package ai.sovereignrag.identity.core.controller

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
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

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
                schema = Schema(implementation = Map::class))]),
        ApiResponse(responseCode = "404", description = "User not found or not locked")
    ])
    @SecurityRequirement(name = "bearerAuth")
    fun unlockUser(
        @Parameter(description = "Username to unlock", example = "john.doe", required = true)
        @PathVariable username: String
    ): ResponseEntity<Any> {
        log.info { "Admin request to unlock user: $username" }
        
        val unlocked = accountLockoutService.unlockAccount(username)
        
        return if (unlocked) {
            log.info { "Successfully unlocked user: $username" }
            ResponseEntity.ok(mapOf(
                "status" to "success",
                "message" to "User account unlocked successfully",
                "username" to username
            ))
        } else {
            log.warn { "User not found or not locked: $username" }
            ResponseEntity.status(404).body(mapOf(
                "status" to "not_found",
                "message" to "User not found or account was not locked",
                "username" to username
            ))
        }
    }

    @PostMapping("/unlock-client/{clientId}")
    @Operation(summary = "Unlock client", description = "Unlock a locked OAuth client")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Client unlocked successfully",
            content = [Content(mediaType = "application/json",
                schema = Schema(implementation = Map::class))]),
        ApiResponse(responseCode = "404", description = "Client not found or not locked")
    ])
    @SecurityRequirement(name = "bearerAuth")
    fun unlockClient(
        @Parameter(description = "Client ID to unlock", example = "client_123", required = true)
        @PathVariable clientId: String
    ): ResponseEntity<Any> {
        log.info { "Admin request to unlock client: $clientId" }
        
        val unlocked = clientLockoutService.unlockClient(clientId)
        
        return if (unlocked) {
            log.info { "Successfully unlocked client: $clientId" }
            ResponseEntity.ok(mapOf(
                "status" to "success",
                "message" to "Client unlocked successfully",
                "clientId" to clientId
            ))
        } else {
            log.warn { "Client not found or not locked: $clientId" }
            ResponseEntity.status(404).body(mapOf(
                "status" to "not_found",
                "message" to "Client not found or was not locked",
                "clientId" to clientId
            ))
        }
    }

    @GetMapping("/lockout-status/user/{username}")
    @Operation(summary = "Get user lockout status", description = "Check if a user account is locked")
    @ApiResponse(responseCode = "200", description = "Lockout status retrieved",
        content = [Content(mediaType = "application/json",
            schema = Schema(implementation = Map::class))])
    @SecurityRequirement(name = "bearerAuth")
    fun getUserLockoutStatus(
        @Parameter(description = "Username to check", example = "john.doe", required = true)
        @PathVariable username: String
    ): ResponseEntity<Any> {
        log.info { "Admin checking lockout status for user: $username" }
        
        val remainingMinutes = accountLockoutService.getRemainingLockoutMinutes(username)
        
        return if (remainingMinutes != null && remainingMinutes > 0) {
            ResponseEntity.ok(mapOf(
                "username" to username,
                "locked" to true,
                "remainingMinutes" to remainingMinutes,
                "message" to "User is locked for $remainingMinutes more minutes"
            ))
        } else {
            ResponseEntity.ok(mapOf(
                "username" to username,
                "locked" to false,
                "message" to "User is not locked"
            ))
        }
    }

    @GetMapping("/lockout-status/client/{clientId}")
    @Operation(summary = "Get client lockout status", description = "Check if an OAuth client is locked")
    @ApiResponse(responseCode = "200", description = "Client lockout status retrieved",
        content = [Content(mediaType = "application/json",
            schema = Schema(implementation = Map::class))])
    @SecurityRequirement(name = "bearerAuth")
    fun getClientLockoutStatus(
        @Parameter(description = "Client ID to check", example = "client_123", required = true)
        @PathVariable clientId: String
    ): ResponseEntity<Any> {
        log.info { "Admin checking lockout status for client: $clientId" }
        
        val remainingMinutes = clientLockoutService.getRemainingLockoutMinutes(clientId)
        
        return if (remainingMinutes != null && remainingMinutes > 0) {
            ResponseEntity.ok(mapOf(
                "clientId" to clientId,
                "locked" to true,
                "remainingMinutes" to remainingMinutes,
                "message" to "Client is locked for $remainingMinutes more minutes"
            ))
        } else {
            ResponseEntity.ok(mapOf(
                "clientId" to clientId,
                "locked" to false,
                "message" to "Client is not locked"
            ))
        }
    }
}