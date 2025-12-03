package ai.sovereignrag.auth.authentication

import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority

class KnowledgeBaseApiKeyAuthenticationToken : AbstractAuthenticationToken {

    private val knowledgeBaseId: String
    private val apiKey: String?

    constructor(knowledgeBaseId: String, apiKey: String) : super(null) {
        this.knowledgeBaseId = knowledgeBaseId
        this.apiKey = apiKey
        isAuthenticated = false
    }

    constructor(knowledgeBaseId: String, authorities: Collection<GrantedAuthority>) : super(authorities) {
        this.knowledgeBaseId = knowledgeBaseId
        this.apiKey = null
        isAuthenticated = true
    }

    override fun getCredentials(): Any? = apiKey

    override fun getPrincipal(): Any = knowledgeBaseId

    fun getKnowledgeBaseId(): String = knowledgeBaseId
}
