package nl.compilot.ai.security.authentication

import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority

/**
 * Custom authentication token for tenant API key authentication
 *
 * This token represents an authentication request using:
 * - Principal: Tenant ID
 * - Credentials: API Key (plain text)
 */
class TenantApiKeyAuthenticationToken : AbstractAuthenticationToken {

    private val tenantId: String
    private val apiKey: String?

    /**
     * Constructor for unauthenticated token (before authentication)
     *
     * @param tenantId The tenant ID attempting to authenticate
     * @param apiKey The plain text API key provided by the client
     */
    constructor(tenantId: String, apiKey: String) : super(null) {
        this.tenantId = tenantId
        this.apiKey = apiKey
        isAuthenticated = false
    }

    /**
     * Constructor for authenticated token (after successful authentication)
     *
     * @param tenantId The authenticated tenant ID
     * @param authorities The granted authorities for this tenant
     */
    constructor(tenantId: String, authorities: Collection<GrantedAuthority>) : super(authorities) {
        this.tenantId = tenantId
        this.apiKey = null
        isAuthenticated = true
    }

    override fun getCredentials(): Any? = apiKey

    override fun getPrincipal(): Any = tenantId

    /**
     * Get the tenant ID from this authentication token
     */
    fun getTenantId(): String = tenantId
}
