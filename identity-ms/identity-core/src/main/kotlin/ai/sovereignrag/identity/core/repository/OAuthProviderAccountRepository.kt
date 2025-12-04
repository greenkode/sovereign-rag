package ai.sovereignrag.identity.core.repository

import ai.sovereignrag.identity.core.entity.OAuthProvider
import ai.sovereignrag.identity.core.entity.OAuthProviderAccount
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface OAuthProviderAccountRepository : JpaRepository<OAuthProviderAccount, UUID> {
    fun findByProviderAndProviderUserId(provider: OAuthProvider, providerUserId: String): OAuthProviderAccount?
    fun findByProviderEmail(providerEmail: String): OAuthProviderAccount?
    fun findByUserId(userId: UUID): List<OAuthProviderAccount>
    fun existsByProviderAndProviderUserId(provider: OAuthProvider, providerUserId: String): Boolean

    @Modifying
    @Query("UPDATE OAuthProviderAccount pa SET pa.providerEmail = :anonymizedEmail, pa.providerUserId = :anonymizedId WHERE pa.user.id = :userId")
    fun anonymizeByUserId(
        @Param("userId") userId: UUID,
        @Param("anonymizedEmail") anonymizedEmail: String,
        @Param("anonymizedId") anonymizedId: String
    ): Int
}
