package ai.sovereignrag.identity.core.deletion.controller

import ai.sovereignrag.commons.security.IsMerchantSuperAdmin
import ai.sovereignrag.identity.core.deletion.command.DeleteOrganizationCommand
import ai.sovereignrag.identity.core.deletion.command.DeleteUserCommand
import ai.sovereignrag.identity.core.deletion.dto.DeleteOrganizationRequest
import ai.sovereignrag.identity.core.deletion.dto.DeleteOrganizationResponse
import ai.sovereignrag.identity.core.deletion.dto.DeleteUserRequest
import ai.sovereignrag.identity.core.deletion.dto.DeleteUserResponse
import an.awesome.pipelinr.Pipeline
import io.github.oshai.kotlinlogging.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/merchant/deletion")
@Tag(name = "Deletion", description = "GDPR-compliant user and organization deletion")
class DeletionController(
    private val pipeline: Pipeline
) {

    @DeleteMapping("/user")
    @IsMerchantSuperAdmin
    @Operation(
        summary = "Delete user",
        description = "GDPR-compliant user deletion - anonymizes PII and disables account"
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "200",
            description = "User deleted successfully",
            content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = DeleteUserResponse::class)
            )]
        ),
        ApiResponse(responseCode = "404", description = "User not found"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions")
    ])
    @SecurityRequirement(name = "bearerAuth")
    fun deleteUser(@RequestBody request: DeleteUserRequest): DeleteUserResponse {
        log.info { "Received request to delete user: ${request.userId}" }

        val result = pipeline.send(
            DeleteUserCommand(userId = UUID.fromString(request.userId))
        )

        return DeleteUserResponse(
            success = result.success,
            message = result.message,
            userId = result.userId
        )
    }

    @DeleteMapping("/organization")
    @IsMerchantSuperAdmin
    @Operation(
        summary = "Delete organization",
        description = "GDPR-compliant organization deletion - anonymizes all users and organization PII"
    )
    @ApiResponses(value = [
        ApiResponse(
            responseCode = "200",
            description = "Organization deleted successfully",
            content = [Content(
                mediaType = "application/json",
                schema = Schema(implementation = DeleteOrganizationResponse::class)
            )]
        ),
        ApiResponse(responseCode = "404", description = "Organization not found"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions")
    ])
    @SecurityRequirement(name = "bearerAuth")
    fun deleteOrganization(@RequestBody request: DeleteOrganizationRequest): DeleteOrganizationResponse {
        log.info { "Received request to delete organization: ${request.organizationId}" }

        val result = pipeline.send(
            DeleteOrganizationCommand(organizationId = UUID.fromString(request.organizationId))
        )

        return DeleteOrganizationResponse(
            success = result.success,
            message = result.message,
            organizationId = result.organizationId,
            usersDeleted = result.usersDeleted
        )
    }
}
