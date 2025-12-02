package ai.sovereignrag.auth

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.Base64

private val logger = KotlinLogging.logger {}

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

        if (token != null && !isRsaSignedToken(token) && jwtTokenProvider.validateToken(token)) {
            val tenantId = jwtTokenProvider.getTenantId(token)
            val authentication = UsernamePasswordAuthenticationToken(tenantId, null, emptyList())
            SecurityContextHolder.getContext().authentication = authentication
            logger.debug { "Tenant JWT authentication successful: $tenantId" }
        }

        // Continue filter chain - Spring Security will handle SecurityContext cleanup
        filterChain.doFilter(request, response)
    }

    private fun isRsaSignedToken(token: String): Boolean =
        runCatching {
            val header = token.substringBefore(".")
            val decoded = String(Base64.getUrlDecoder().decode(header))
            decoded.contains("\"RS256\"") || decoded.contains("\"RS384\"") || decoded.contains("\"RS512\"")
        }.getOrDefault(false)

    private fun resolveToken(request: HttpServletRequest): String? =
        request.getHeader("Authorization")?.takeIf { it.startsWith("Bearer ") }?.substring(7)
}
