package ai.sovereignrag.tenant.service

import nl.compilot.ai.commons.tenant.TenantRegistry as TenantRegistryInterface
import nl.compilot.ai.tenant.domain.Tenant

/**
 * Tenant Registry Service - manages tenant lookups and validation
 * Extends the interface from commons with additional methods
 */
interface TenantRegistry : TenantRegistryInterface {
    /**
     * Validate tenant credentials
     * @param tenantId The tenant ID from X-Tenant-ID header
     * @param apiKey The API key from X-API-Key header
     * @return Tenant if valid, null otherwise
     */
    fun validateTenant(tenantId: String, apiKey: String): Tenant?

    /**
     * Get tenant by ID (overrides interface to return concrete Tenant type)
     * @param tenantId The tenant ID
     * @return Tenant
     * @throws TenantNotFoundException if tenant not found
     */
    override fun getTenant(tenantId: String): Tenant
}
