package ai.sovereignrag.identity.core.repository

import ai.sovereignrag.identity.core.entity.OAuthRegisteredClient
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OAuthRegisteredClientRepository : JpaRepository<OAuthRegisteredClient, String> {
    fun findByClientId(clientId: String): OAuthRegisteredClient?
    fun findByDomain(domain: String): OAuthRegisteredClient?
    fun existsByDomain(domain: String): Boolean
}