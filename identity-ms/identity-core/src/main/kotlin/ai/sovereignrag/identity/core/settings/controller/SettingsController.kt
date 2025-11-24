package ai.sovereignrag.identity.core.settings.controller

import ai.sovereignrag.identity.commons.permissions.IsMerchantSuperAdmin
import ai.sovereignrag.identity.core.settings.command.EnableProductionCommand
import ai.sovereignrag.identity.core.settings.command.ResetKeysCommand
import ai.sovereignrag.identity.core.settings.command.SwitchEnvironmentCommand
import ai.sovereignrag.identity.core.settings.command.UpdateAlertsCommand
import ai.sovereignrag.identity.core.settings.command.UpdateUserNameCommand
import ai.sovereignrag.identity.core.settings.dto.EnableProductionRequest
import ai.sovereignrag.identity.core.settings.dto.EnableProductionResponse
import ai.sovereignrag.identity.core.settings.dto.EnvironmentStatusResponse
import ai.sovereignrag.identity.core.settings.dto.ResetKeysRequest
import ai.sovereignrag.identity.core.settings.dto.SwitchEnvironmentRequest
import ai.sovereignrag.identity.core.settings.dto.ResetKeysResponse
import ai.sovereignrag.identity.core.settings.dto.UpdateAlertsRequest
import ai.sovereignrag.identity.core.settings.dto.UpdateAlertsResponse
import ai.sovereignrag.identity.core.settings.dto.UpdateUserNameRequest
import ai.sovereignrag.identity.core.settings.dto.UpdateUserNameResponse
import ai.sovereignrag.identity.core.settings.dto.UserSettingsResponse
import ai.sovereignrag.identity.core.settings.query.GetUserSettingsQuery
import an.awesome.pipelinr.Pipeline
import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/merchant/settings")
@Tag(name = "Settings", description = "User and merchant settings management")
class SettingsController(
    private val pipeline: Pipeline
) {

    @GetMapping("/user")
    @Operation(summary = "Get user settings", description = "Retrieve current user settings")
    @ApiResponse(responseCode = "200", description = "User settings retrieved",
        content = [Content(mediaType = "application/json",
            schema = Schema(implementation = UserSettingsResponse::class))])
    @SecurityRequirement(name = "bearerAuth")
    fun getUserSettings(): UserSettingsResponse {
        log.info { "Getting user settings" }

        val result = pipeline.send(GetUserSettingsQuery())

        return UserSettingsResponse(
            firstName = result.firstName,
            lastName = result.lastName,
            email = result.email,
            roles = result.roles,
            merchantName = result.merchantName
        )
    }

    @PutMapping("/user")
    @Operation(summary = "Update user name", description = "Update user's first and last name")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "User name updated successfully",
            content = [Content(mediaType = "application/json",
                schema = Schema(implementation = UpdateUserNameResponse::class))]),
        ApiResponse(responseCode = "400", description = "Invalid request data")
    ])
    @SecurityRequirement(name = "bearerAuth")
    fun updateUserName(@RequestBody request: UpdateUserNameRequest): UpdateUserNameResponse {
        log.info { "Updating user name" }

        val result = pipeline.send(UpdateUserNameCommand(
            firstName = request.firstName,
            lastName = request.lastName
        ))

        return UpdateUserNameResponse(
            success = result.success,
            message = result.message
        )
    }

    @PostMapping("/reset-keys")
    @IsMerchantSuperAdmin
    @Operation(summary = "Reset merchant keys", description = "Generate new client secret for merchant in specified environment")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Keys reset successfully",
            content = [Content(mediaType = "application/json",
                schema = Schema(implementation = ResetKeysResponse::class))]),
        ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        ApiResponse(responseCode = "400", description = "Invalid environment or insufficient access")
    ])
    @SecurityRequirement(name = "bearerAuth")
    fun resetKeys(@RequestBody(required = false) request: ResetKeysRequest): ResetKeysResponse {
        log.info { "Resetting merchant keys for environment: ${request.environment}" }

        val result = pipeline.send(ResetKeysCommand(
            environment = request.environment
        ))

        return ResetKeysResponse(
            clientId = result.clientId,
            clientSecret = result.clientSecret,
            environment = result.environment,
            success = result.success,
            message = result.message
        )
    }

    @PutMapping("/alerts")
    @IsMerchantSuperAdmin
    @Operation(summary = "Update alert settings", description = "Update merchant alert thresholds")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Alerts updated successfully",
            content = [Content(mediaType = "application/json",
                schema = Schema(implementation = UpdateAlertsResponse::class))]),
        ApiResponse(responseCode = "400", description = "Invalid request data"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions")
    ])
    @SecurityRequirement(name = "bearerAuth")
    fun updateAlerts(@RequestBody request: UpdateAlertsRequest): UpdateAlertsResponse {
        log.info { "Updating alert settings" }

        val result = pipeline.send(UpdateAlertsCommand(
            failureLimit = request.failureLimit,
            lowBalance = request.lowBalance
        ))

        return UpdateAlertsResponse(
            success = result.success,
            message = result.message
        )
    }

    @PostMapping("/switch-environment")
    @Operation(summary = "Switch environment", description = "Switch between SANDBOX and PRODUCTION environments and get a new token")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Environment switched successfully with new token",
            content = [Content(mediaType = "application/json",
                schema = Schema(implementation = EnvironmentStatusResponse::class))]),
        ApiResponse(responseCode = "400", description = "Invalid environment or insufficient permissions"),
        ApiResponse(responseCode = "403", description = "Cannot switch to requested environment")
    ])
    @SecurityRequirement(name = "bearerAuth")
    fun switchEnvironment(@RequestBody request: SwitchEnvironmentRequest): EnvironmentStatusResponse {
        log.info { "Switching to environment: ${request.environment}" }

        return pipeline.send(SwitchEnvironmentCommand(
            environment = request.environment
        ))
    }

    @PostMapping("/enable-production")
    @IsMerchantSuperAdmin
    @Operation(summary = "Enable production mode", description = "Enable or disable production mode for the merchant")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "Production mode updated successfully",
            content = [Content(mediaType = "application/json",
                schema = Schema(implementation = EnableProductionResponse::class))]),
        ApiResponse(responseCode = "400", description = "Invalid environment mode"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions")
    ])
    @SecurityRequirement(name = "bearerAuth")
    fun enableProduction(@RequestBody request: EnableProductionRequest): EnableProductionResponse {
        log.info { "Updating merchant environment to: ${request.environmentMode}" }

        val result = pipeline.send(EnableProductionCommand(
            environmentMode = request.environmentMode
        ))

        return EnableProductionResponse(
            merchantId = result.merchantId,
            environmentMode = result.environmentMode,
            affectedUsers = result.affectedUsers,
            success = result.success,
            message = result.message
        )
    }
}