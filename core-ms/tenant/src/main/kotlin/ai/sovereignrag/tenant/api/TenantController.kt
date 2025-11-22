package ai.sovereignrag.tenant.api

import mu.KotlinLogging
import ai.sovereignrag.tenant.domain.Tenant
import ai.sovereignrag.commons.tenant.TenantStatus
import ai.sovereignrag.tenant.service.TenantRegistryService
import ai.sovereignrag.tenant.service.ApiKeyResetService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

private val logger = KotlinLogging.logger {}

/**
 * Tenant Management API
 * Endpoints for creating, listing, and managing tenants
 *
 * TODO: Add authentication/authorization in production
 */
@RestController
@RequestMapping("/api/admin/tenants")
class TenantController(
    private val tenantRegistry: TenantRegistryService,
    private val apiKeyResetService: ApiKeyResetService
) {

    /**
     * Create a new tenant
     */
    @PostMapping
    fun createTenant(@RequestBody request: CreateTenantRequest): ResponseEntity<CreateTenantResponse> {
        logger.info { "Creating tenant: ${request.tenantId}" }

        return try {
            val result = tenantRegistry.createTenant(
                tenantId = request.tenantId,
                name = request.name,
                contactEmail = request.contactEmail,
                contactName = request.contactName,
                wordpressUrl = request.wordpressUrl
            )

            ResponseEntity.ok(CreateTenantResponse(
                success = true,
                tenant = TenantDto.from(result.tenant),
                apiKey = result.apiKey,
                message = "Tenant created successfully"
            ))
        } catch (e: Exception) {
            logger.error(e) { "Failed to create tenant: ${request.tenantId}" }
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                CreateTenantResponse(
                    success = false,
                    message = "Failed to create tenant: ${e.message}"
                )
            )
        }
    }

    /**
     * List all tenants
     */
    @GetMapping
    fun listTenants(@RequestParam(required = false) status: TenantStatus?): List<TenantDto> {
        return tenantRegistry.listTenants(status).map { TenantDto.from(it) }
    }

    /**
     * Get tenant by ID
     */
    @GetMapping("/{tenantId}")
    fun getTenant(@PathVariable tenantId: String): ResponseEntity<TenantDto> {
        return try {
            val tenant = tenantRegistry.getTenant(tenantId)
            ResponseEntity.ok(TenantDto.from(tenant))
        } catch (e: Exception) {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Step 1: Request API key reset
     * Generates a secure token and sends it to the admin email
     */
    @PostMapping("/{tenantId}/request-reset")
    fun requestApiKeyReset(
        @PathVariable tenantId: String,
        request: HttpServletRequest
    ): ResponseEntity<ApiKeyResetRequestResponse> {
        val ipAddress = request.remoteAddr
        logger.info { "API key reset request for tenant: $tenantId from IP: $ipAddress" }

        return try {
            val result = apiKeyResetService.requestReset(tenantId, ipAddress)

            ResponseEntity.ok(ApiKeyResetRequestResponse(
                success = result.success,
                message = result.message,
                maskedEmail = result.maskedEmail
            ))
        } catch (e: Exception) {
            logger.error(e) { "Failed to request API key reset for tenant: $tenantId" }
            val status = when (e) {
                is org.springframework.web.server.ResponseStatusException -> e.statusCode.value()
                else -> HttpStatus.INTERNAL_SERVER_ERROR.value()
            }
            ResponseEntity.status(status).body(
                ApiKeyResetRequestResponse(
                    success = false,
                    message = e.message ?: "Failed to request API key reset"
                )
            )
        }
    }

    /**
     * Step 2: Confirm API key reset with token
     * Validates the token and regenerates the API key
     */
    @PostMapping("/{tenantId}/confirm-reset")
    fun confirmApiKeyReset(
        @PathVariable tenantId: String,
        @RequestBody request: ApiKeyResetConfirmRequest
    ): ResponseEntity<ApiKeyResetConfirmResponse> {
        logger.info { "API key reset confirmation for tenant: $tenantId" }

        return try {
            val result = apiKeyResetService.confirmReset(tenantId, request.token)

            ResponseEntity.ok(ApiKeyResetConfirmResponse(
                success = result.success,
                message = result.message,
                newApiKey = result.newApiKey
            ))
        } catch (e: Exception) {
            logger.error(e) { "Failed to confirm API key reset for tenant: $tenantId" }
            val status = when (e) {
                is org.springframework.web.server.ResponseStatusException -> e.statusCode.value()
                else -> HttpStatus.INTERNAL_SERVER_ERROR.value()
            }
            ResponseEntity.status(status).body(
                ApiKeyResetConfirmResponse(
                    success = false,
                    message = e.message ?: "Failed to confirm API key reset"
                )
            )
        }
    }

    /**
     * Delete tenant (soft delete by default)
     */
    @DeleteMapping(
        "/{tenantId}")
    fun deleteTenant(
        @PathVariable tenantId: String,
        @RequestParam(defaultValue = "false") hardDelete: Boolean
    ): ResponseEntity<Map<String, Any>> {
        return try {
            tenantRegistry.deleteTenant(tenantId, hardDelete)
            ResponseEntity.ok(mapOf(
                "success" to true,
                "message" to if (hardDelete) "Tenant permanently deleted" else "Tenant soft deleted"
            ))
        } catch (e: Exception) {
            logger.error(e) { "Failed to delete tenant: $tenantId" }
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf(
                "success" to false,
                "message" to "Failed to delete tenant: ${e.message}"
            ))
        }
    }
}

// ============================================
// DTOs
// ============================================

data class CreateTenantRequest(
    val tenantId: String,
    val name: String,
    val contactEmail: String? = null,
    val contactName: String? = null,
    val wordpressUrl: String? = null
)

data class CreateTenantResponse(
    val success: Boolean,
    val tenant: TenantDto? = null,
    val apiKey: String? = null,  // Only returned on successful creation!
    val message: String? = null
)

data class RegenerateApiKeyResponse(
    val success: Boolean,
    val tenantId: String,
    val newApiKey: String? = null,  // Only returned on successful regeneration!
    val message: String? = null
)

data class ApiKeyResetRequestResponse(
    val success: Boolean,
    val message: String,
    val maskedEmail: String? = null
)

data class ApiKeyResetConfirmRequest(
    val token: String
)

data class ApiKeyResetConfirmResponse(
    val success: Boolean,
    val message: String,
    val newApiKey: String? = null  // Only returned on successful confirmation!
)

data class TenantDto(
    val id: String,
    val name: String,
    val databaseName: String,
    val status: TenantStatus,
    val maxDocuments: Int,
    val maxEmbeddings: Int,
    val maxRequestsPerDay: Int,
    val subscriptionTier: String,
    val contactEmail: String?,
    val contactName: String?,
    val wordpressUrl: String?,
    val wordpressVersion: String?,
    val pluginVersion: String?,
    val createdAt: String,
    val updatedAt: String,
    val lastActiveAt: String?
) {
    companion object {
        fun from(tenant: Tenant): TenantDto {
            return TenantDto(
                id = tenant.id,
                name = tenant.name,
                databaseName = tenant.databaseName,
                status = tenant.status,
                maxDocuments = tenant.maxDocuments,
                maxEmbeddings = tenant.maxEmbeddings,
                maxRequestsPerDay = tenant.maxRequestsPerDay,
                subscriptionTier = tenant.subscriptionTier,
                contactEmail = tenant.contactEmail,
                contactName = tenant.contactName,
                wordpressUrl = tenant.wordpressUrl,
                wordpressVersion = tenant.wordpressVersion,
                pluginVersion = tenant.pluginVersion,
                createdAt = tenant.createdAt.toString(),
                updatedAt = tenant.updatedAt.toString(),
                lastActiveAt = tenant.lastActiveAt?.toString()
            )
        }
    }
}
