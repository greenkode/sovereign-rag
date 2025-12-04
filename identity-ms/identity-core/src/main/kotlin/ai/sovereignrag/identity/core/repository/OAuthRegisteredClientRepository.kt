package ai.sovereignrag.identity.core.repository

import ai.sovereignrag.identity.core.entity.OAuthRegisteredClient
import ai.sovereignrag.identity.core.entity.OrganizationStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface OAuthRegisteredClientRepository : JpaRepository<OAuthRegisteredClient, String> {
    fun findByClientId(clientId: String): OAuthRegisteredClient?
    fun findByDomain(domain: String): OAuthRegisteredClient?
    fun existsByDomain(domain: String): Boolean

    @Query("SELECT c FROM OAuthRegisteredClient c LEFT JOIN FETCH c.settings WHERE c.id = :id")
    fun findByIdWithSettings(id: String): Optional<OAuthRegisteredClient>

    fun findByKnowledgeBaseIdAndStatus(knowledgeBaseId: String, status: OrganizationStatus): OAuthRegisteredClient?

    fun findByClientIdAndKnowledgeBaseIdIsNotNullAndStatus(clientId: String, status: OrganizationStatus): OAuthRegisteredClient?

    fun findByOrganizationIdAndKnowledgeBaseIdIsNotNull(organizationId: UUID): List<OAuthRegisteredClient>

    fun findByOrganizationId(organizationId: UUID): List<OAuthRegisteredClient>
}