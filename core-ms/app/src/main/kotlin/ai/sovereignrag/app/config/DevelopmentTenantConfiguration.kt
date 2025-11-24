package ai.sovereignrag.app.config

import jakarta.annotation.PostConstruct
import io.github.oshai.kotlinlogging.KotlinLogging
import ai.sovereignrag.tenant.service.TenantRegistryService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

private val logger = KotlinLogging.logger {}

/**
 * Configuration that ensures a default development tenant exists
 * This allows development and testing without manually creating tenants
 */
@Configuration
class DevelopmentTenantConfiguration(
    private val tenantRegistryService: TenantRegistryService,
    @Value("\${sovereignrag.dev-tenant.enabled:true}") private val devTenantEnabled: Boolean,
    @Value("\${sovereignrag.dev-tenant.id:dev}") private val devTenantId: String,
    @Value("\${sovereignrag.dev-tenant.name:Development Tenant}") private val devTenantName: String,
    @Value("\${sovereignrag.dev-tenant.api-key:dev-api-key-12345}") private val devApiKey: String
) {

    @PostConstruct
    fun initializeDevTenant() {
        if (!devTenantEnabled) {
            logger.info { "Development tenant initialization is disabled" }
            return
        }

        try {
            // Check if dev tenant already exists
            val existingTenant = try {
                tenantRegistryService.getTenant(devTenantId)
            } catch (e: Exception) {
                // Tenant doesn't exist (could be TenantNotFoundException or EmptyResultDataAccessException)
                null
            }

            if (existingTenant != null) {
                logger.info { "Development tenant '$devTenantId' already exists" }
                logger.info { "  Tenant Name: ${existingTenant.name}" }
                logger.info { "  Database: ${existingTenant.databaseName}" }
                logger.info { "  Status: ${existingTenant.status}" }
                // Don't return here - continue to apply migrations
            } else {

            // Create development tenant
            logger.info { "Creating development tenant '$devTenantId'..." }
            val result = tenantRegistryService.createTenant(
                tenantId = devTenantId,
                name = devTenantName,
                clientId = "dev-client-id",
                contactEmail = "dev@sovereignrag.local",
                contactName = "Development",
                wordpressUrl = "http://localhost:8080"
            )

            logger.info { "✓ Development tenant created successfully!" }
            logger.info { "  Tenant ID: ${result.tenant.id}" }
            logger.info { "  Tenant Name: ${result.tenant.name}" }
            logger.info { "  Database: ${result.tenant.databaseName}" }
            logger.info { "  API Key: ${result.apiKey}" }
            logger.info { "" }
            logger.info { "Configure your WordPress plugin with:" }
            logger.info { "  X-Tenant-ID: ${result.tenant.id}" }
            logger.info { "  X-API-Key: ${result.apiKey}" }
                logger.info { "" }
                logger.warn { "⚠️  SAVE THIS API KEY - It will not be shown again!" }
            }

        } catch (e: Exception) {
            logger.error(e) { "Failed to initialize development tenant" }
            // Don't fail application startup if dev tenant creation fails
        }

        // Apply pending migrations to all existing tenants
        try {
            logger.info { "Checking for pending tenant database migrations..." }
            tenantRegistryService.migrateAllTenants()
        } catch (e: Exception) {
            logger.error(e) { "Failed to migrate tenant databases" }
        }
    }
}
