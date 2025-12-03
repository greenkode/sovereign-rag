package ai.sovereignrag.auth.api

import ai.sovereignrag.auth.JwtTokenProvider
import ai.sovereignrag.auth.authentication.KnowledgeBaseApiKeyAuthenticationToken
import ai.sovereignrag.commons.dto.AuthRequest
import ai.sovereignrag.commons.dto.AuthResponse
import ai.sovereignrag.commons.knowledgebase.KnowledgeBaseRegistry
import ai.sovereignrag.commons.knowledgebase.KnowledgeBaseStatus
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val jwtTokenProvider: JwtTokenProvider,
    private val knowledgeBaseRegistry: KnowledgeBaseRegistry,
    private val authenticationManager: AuthenticationManager
) {

    @PostMapping("/authenticate")
    fun authenticate(@RequestBody request: AuthRequest): AuthResponse {
        log.info { "Authentication request for knowledge base: ${request.knowledgeBaseId}" }

        try {
            val authRequest = KnowledgeBaseApiKeyAuthenticationToken(request.knowledgeBaseId, request.apiKey)
            val authentication = authenticationManager.authenticate(authRequest)
            val knowledgeBaseId = authentication.principal as String

            val kb = knowledgeBaseRegistry.getKnowledgeBase(knowledgeBaseId)
            if (kb.status != KnowledgeBaseStatus.ACTIVE) {
                log.warn { "Knowledge base $knowledgeBaseId is not active: ${kb.status}" }
                throw ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Knowledge base is ${kb.status.name.lowercase()}"
                )
            }

            val token = jwtTokenProvider.createToken(knowledgeBaseId)
            val expiresIn = jwtTokenProvider.getExpirationTimeInSeconds()

            log.info { "JWT token generated for knowledge base: $knowledgeBaseId (expires in ${expiresIn}s)" }

            return AuthResponse(
                token = token,
                expiresIn = expiresIn.toInt(),
                knowledgeBaseId = knowledgeBaseId
            )
        } catch (e: BadCredentialsException) {
            log.warn { "Invalid credentials for knowledge base: ${request.knowledgeBaseId}" }
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials")
        } catch (e: ResponseStatusException) {
            throw e
        } catch (e: Exception) {
            log.error(e) { "Authentication failed for knowledge base: ${request.knowledgeBaseId}" }
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Authentication failed")
        }
    }

    @PostMapping("/refresh")
    fun refresh(authentication: Authentication): AuthResponse {
        val knowledgeBaseId = authentication.principal as String
        log.info { "Token refresh request for knowledge base: $knowledgeBaseId" }

        val kb = runCatching { knowledgeBaseRegistry.getKnowledgeBase(knowledgeBaseId) }
            .getOrElse { e ->
                log.error(e) { "Error fetching knowledge base: $knowledgeBaseId" }
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Token refresh failed")
            }

        if (kb.status != KnowledgeBaseStatus.ACTIVE) {
            log.warn { "Knowledge base $knowledgeBaseId is not active: ${kb.status}" }
            throw ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Knowledge base is ${kb.status.name.lowercase()}"
            )
        }

        val token = jwtTokenProvider.createToken(kb.id)
        val expiresIn = jwtTokenProvider.getExpirationTimeInSeconds()

        log.info { "JWT token refreshed for knowledge base: ${kb.id} (expires in ${expiresIn}s)" }

        return AuthResponse(
            token = token,
            expiresIn = expiresIn.toInt(),
            knowledgeBaseId = kb.id
        )
    }
}
