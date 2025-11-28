package ai.sovereignrag.auth

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Instant

private val log = KotlinLogging.logger {}

@Component
class OrganizationSetupFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val objectMapper: ObjectMapper
) : OncePerRequestFilter() {

    private val excludedPaths = setOf(
        "/api/auth",
        "/actuator/health",
        "/actuator/info"
    )

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val path = request.requestURI

        if (isExcludedPath(path)) {
            filterChain.doFilter(request, response)
            return
        }

        val token = resolveToken(request)

        if (token != null && jwtTokenProvider.validateToken(token)) {
            val setupCompleted = jwtTokenProvider.isSetupCompleted(token)

            if (!setupCompleted) {
                log.warn { "Organization setup not completed for request: ${request.method} $path" }
                writeErrorResponse(response, request)
                return
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun isExcludedPath(path: String): Boolean {
        return excludedPaths.any { path.startsWith(it) }
    }

    private fun resolveToken(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")
        return bearerToken?.takeIf { it.startsWith("Bearer ") }?.substring(7)
    }

    private fun writeErrorResponse(response: HttpServletResponse, request: HttpServletRequest) {
        response.status = HttpStatus.PRECONDITION_REQUIRED.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE

        val errorResponse = mapOf(
            "timestamp" to Instant.now().toString(),
            "status" to HttpStatus.PRECONDITION_REQUIRED.value(),
            "error" to "ORGANIZATION_SETUP_REQUIRED",
            "message" to "Organization setup is required before accessing this resource",
            "path" to request.requestURI
        )

        response.writer.write(objectMapper.writeValueAsString(errorResponse))
    }
}
