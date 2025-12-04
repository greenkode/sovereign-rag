package ai.sovereignrag.identity.core.repository

import ai.sovereignrag.identity.core.entity.OAuthUser
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface OAuthUserRepository : JpaRepository<OAuthUser, UUID> {
    fun findByUsername(username: String): OAuthUser?
    fun findByEmail(email: String): OAuthUser?
    fun findByMerchantId(merchantId: UUID): List<OAuthUser>
    fun findByOrganizationId(organizationId: UUID): List<OAuthUser>
    fun existsByEmail(email: String): Boolean

    @Query("SELECT u FROM OAuthUser u JOIN u.authorities a WHERE u.merchantId = :merchantId AND a = 'ROLE_SUPER_ADMIN'")
    fun findSuperAdminsByMerchantId(merchantId: UUID): List<OAuthUser>
}