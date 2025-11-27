package ai.sovereignrag.commons.tenant

import org.springframework.security.core.context.SecurityContextHolder

/**
 * Tenant context that delegates to Spring Security's SecurityContext
 *
 * This approach provides several benefits over ThreadLocal:
 * 1. Automatic propagation to background threads when configured
 * 2. No manual cleanup required (Spring Security handles it)
 * 3. Integration with Spring Security's authentication mechanism
 * 4. Thread-safe by design
 *
 * The tenant ID is stored as the principal in Spring Security's Authentication object.
 * It is set by JwtAuthenticationFilter during request authentication.
 */
object TenantContext {

    /**
     * Get the current tenant ID from Spring Security context
     * @throws TenantContextException if no tenant context is set
     */
    fun getCurrentTenant(): String {
        return getCurrentTenantOrNull()
            ?: throw TenantContextException("No tenant context set - user not authenticated")
    }

    /**
     * Get the current tenant ID from Spring Security context or null if not set
     */
    fun getCurrentTenantOrNull(): String? {
        val authentication = SecurityContextHolder.getContext().authentication
        return if (authentication != null && authentication.isAuthenticated) {
            authentication.principal as? String
        } else {
            null
        }
    }
}

class TenantContextException(message: String) : RuntimeException(message)
