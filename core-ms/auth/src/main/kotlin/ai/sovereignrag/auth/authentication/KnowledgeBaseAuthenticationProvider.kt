package ai.sovereignrag.auth.authentication

import ai.sovereignrag.commons.knowledgebase.KnowledgeBaseRegistry
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class KnowledgeBaseAuthenticationProvider(
    private val knowledgeBaseRegistry: KnowledgeBaseRegistry
) : AuthenticationProvider {

    override fun authenticate(authentication: Authentication): Authentication {
        require(authentication is KnowledgeBaseApiKeyAuthenticationToken) {
            "Unsupported authentication type: ${authentication.javaClass.name}"
        }

        val knowledgeBaseId = authentication.getKnowledgeBaseId()
        val apiKey = authentication.credentials as? String
            ?: throw BadCredentialsException("API key is required")

        log.debug { "Authenticating knowledge base: $knowledgeBaseId" }

        val kb = knowledgeBaseRegistry.validateApiKey(knowledgeBaseId, apiKey)
            ?: run {
                log.warn { "Invalid API key for knowledge base: $knowledgeBaseId" }
                throw BadCredentialsException("Invalid API key for knowledge base: $knowledgeBaseId")
            }

        log.info { "Successfully authenticated knowledge base: $knowledgeBaseId" }

        val authorities = listOf(SimpleGrantedAuthority("ROLE_KNOWLEDGE_BASE"))
        return KnowledgeBaseApiKeyAuthenticationToken(knowledgeBaseId, authorities)
    }

    override fun supports(authentication: Class<*>): Boolean {
        return KnowledgeBaseApiKeyAuthenticationToken::class.java.isAssignableFrom(authentication)
    }
}
