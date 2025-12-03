package ai.sovereignrag.auth

import ai.sovereignrag.commons.knowledgebase.KnowledgeBaseContext
import ai.sovereignrag.commons.knowledgebase.KnowledgeBaseContextData
import ai.sovereignrag.commons.knowledgebase.KnowledgeBaseRegistry
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

private val log = KotlinLogging.logger {}

@Component
class KnowledgeBaseContextFilter(
    private val knowledgeBaseRegistry: KnowledgeBaseRegistry
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            setupKnowledgeBaseContext()
            filterChain.doFilter(request, response)
        } finally {
            KnowledgeBaseContext.clear()
        }
    }

    private fun setupKnowledgeBaseContext() {
        val authentication = SecurityContextHolder.getContext().authentication

        if (authentication !is JwtAuthenticationToken) {
            return
        }

        val jwt = authentication.token
        val clientType = jwt.getClaimAsString("client_type")

        when (clientType) {
            "kb_api" -> setupFromKBApiToken(jwt)
            else -> log.debug { "Non-KB API token, skipping KB context setup" }
        }
    }

    private fun setupFromKBApiToken(jwt: org.springframework.security.oauth2.jwt.Jwt) {
        val knowledgeBaseId = jwt.getClaimAsString("knowledge_base_id")
        val organizationIdStr = jwt.getClaimAsString("organization_id")

        if (knowledgeBaseId == null || organizationIdStr == null) {
            log.warn { "KB API token missing required claims" }
            return
        }

        val organizationId = runCatching { UUID.fromString(organizationIdStr) }.getOrNull()
            ?: run {
                log.warn { "Invalid organization_id format: $organizationIdStr" }
                return
            }

        runCatching {
            val kb = knowledgeBaseRegistry.getKnowledgeBase(knowledgeBaseId)

            if (kb.organizationId != organizationId) {
                log.warn { "Knowledge base $knowledgeBaseId does not belong to organization $organizationId" }
                return
            }

            val contextData = KnowledgeBaseContextData(
                knowledgeBaseId = knowledgeBaseId,
                organizationId = organizationId,
                schemaName = kb.schemaName
            )

            KnowledgeBaseContext.setContext(contextData)
            log.debug { "KB context set: kb=$knowledgeBaseId, org=$organizationId, schema=${kb.schemaName}" }
        }.onFailure { e ->
            log.error(e) { "Failed to setup KB context for: $knowledgeBaseId" }
        }
    }
}
