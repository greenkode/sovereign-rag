package ai.sovereignrag.identity.core.invitation.controller

import ai.sovereignrag.identity.core.invitation.dto.CreateMerchantCommand
import ai.sovereignrag.identity.core.invitation.dto.CreateMerchantRequest
import ai.sovereignrag.identity.core.invitation.dto.CreateMerchantResponse
import an.awesome.pipelinr.Pipeline
import mu.KotlinLogging
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/admin/merchants")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Merchant Management", description = "Admin operations for merchant management")
class MerchantController(
    private val pipeline: Pipeline
) {
    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create merchant", description = "Create a new merchant with admin user")
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "Merchant created successfully",
            content = [Content(mediaType = "application/json",
                schema = Schema(implementation = CreateMerchantResponse::class))]),
        ApiResponse(responseCode = "400", description = "Invalid request data"),
        ApiResponse(responseCode = "403", description = "Insufficient permissions")
    ])
    @SecurityRequirement(name = "bearerAuth")
    fun createMerchant(@RequestBody request: CreateMerchantRequest): CreateMerchantResponse {

        log.info { "Creating new merchant: ${request.clientName}" }
        
        val result = pipeline.send(CreateMerchantCommand(request.clientName, request.adminEmail))

        return CreateMerchantResponse(result.success, result.message, result.id)
    }
}