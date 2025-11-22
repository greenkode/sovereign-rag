package ai.sovereignrag.auth

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mu.KotlinLogging
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

private val logger = KotlinLogging.logger {}

/**
 * JWT Authentication Filter
 *
 * Intercepts requests with JWT tokens in Authorization header,
 * validates the token, and sets up Spring Security context with tenant information
 *
 * Authorization header format: "Bearer <jwt-token>"
 *
 * The tenant ID is stored as the principal in the Authentication object,
 * which can be retrieved via SecurityContextHolder throughout the application,
 * including in background threads when properly configured.
 */
@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = resolveToken(request)

        if (token != null && jwtTokenProvider.validateToken(token)) {
            val tenantId = jwtTokenProvider.getTenantId(token)

            // Set authentication in Spring Security context with tenant ID as principal
            // This automatically makes the tenant context available throughout the request,
            // including in background threads when SecurityContext propagation is configured
            val authentication = UsernamePasswordAuthenticationToken(
                tenantId, // principal (tenant ID)
                null,     // credentials (not needed after authentication)
                emptyList() // authorities (can add tenant-specific roles later)
            )
            SecurityContextHolder.getContext().authentication = authentication

            logger.debug { "JWT authentication successful for tenant: $tenantId (${request.method} ${request.requestURI})" }
        } else if (token != null) {
            logger.warn { "Invalid JWT token in request: ${request.method} ${request.requestURI}" }
        }

        // Continue filter chain - Spring Security will handle SecurityContext cleanup
        filterChain.doFilter(request, response)
    }

    /**
     * Extract JWT token from Authorization header
     *
     * Expected format: "Bearer <jwt-token>"
     *
     * @return JWT token string or null if not present/invalid format
     */
    private fun resolveToken(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")
        return if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            bearerToken.substring(7)
        } else {
            null
        }
    }
}
