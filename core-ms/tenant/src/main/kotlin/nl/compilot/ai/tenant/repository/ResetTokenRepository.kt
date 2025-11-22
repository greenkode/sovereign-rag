package nl.compilot.ai.tenant.repository

import nl.compilot.ai.tenant.domain.ResetToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

/**
 * Repository for Reset Token management
 */
@Repository
interface ResetTokenRepository : JpaRepository<ResetToken, String> {

    /**
     * Find all valid (unused and unexpired) tokens for a tenant
     */
    fun findByTenantIdAndUsedAtIsNullAndExpiresAtAfter(
        tenantId: String,
        now: Instant
    ): List<ResetToken>

    /**
     * Find token by hash
     */
    fun findByTokenHash(tokenHash: String): ResetToken?

    /**
     * Delete all expired tokens (cleanup)
     */
    @Modifying
    @Query("DELETE FROM ResetToken r WHERE r.expiresAt < :now")
    fun deleteExpiredTokens(now: Instant): Int

    /**
     * Delete all tokens for a tenant (used when API key is successfully reset)
     */
    @Modifying
    fun deleteByTenantId(tenantId: String): Int
}
