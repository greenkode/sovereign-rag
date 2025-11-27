package ai.sovereignrag.commons.tenant

/**
 * Tenant Registry Service - manages tenant lookups and validation
 *
 * This interface is defined in commons so it can be used by multiple modules
 * without circular dependencies. Implementations are provided in specific modules.
 */
interface TenantRegistry {
    /**
     * Get tenant by ID
     * @param tenantId The tenant ID
     * @return TenantInfo
     * @throws TenantNotFoundException if tenant not found
     */
    fun getTenant(tenantId: String): TenantInfo

    /**
     * Update last active timestamp for tenant
     * @param tenantId The tenant ID
     */
    fun updateLastActive(tenantId: String)
}

class TenantNotFoundException(message: String) : RuntimeException(message)
