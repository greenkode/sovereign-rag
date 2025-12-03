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

private val log = KotlinLogging.logger {}

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
            val knowledgeBaseId = jwtTokenProvider.getKnowledgeBaseId(token)
            val authentication = UsernamePasswordAuthenticationToken(knowledgeBaseId, null, emptyList())
            SecurityContextHolder.getContext().authentication = authentication
            log.debug { "Knowledge base JWT authentication successful: $knowledgeBaseId" }
        }

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
