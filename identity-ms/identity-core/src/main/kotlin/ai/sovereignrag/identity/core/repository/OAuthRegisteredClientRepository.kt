package ai.sovereignrag.identity.core.repository

import ai.sovereignrag.identity.core.entity.OAuthRegisteredClient
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface OAuthRegisteredClientRepository : JpaRepository<OAuthRegisteredClient, String> {
    fun findByClientId(clientId: String): OAuthRegisteredClient?
    fun findByDomain(domain: String): OAuthRegisteredClient?
    fun existsByDomain(domain: String): Boolean

    @Query("SELECT c FROM OAuthRegisteredClient c LEFT JOIN FETCH c.settings WHERE c.id = :id")
    fun findByIdWithSettings(id: String): Optional<OAuthRegisteredClient>
}