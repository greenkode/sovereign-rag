package ai.sovereignrag.identity.core.refreshtoken.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.Optional
import java.util.UUID

@Repository
interface RefreshTokenRepository : JpaRepository<RefreshTokenEntity, UUID> {

    fun findByJti(jti: String): Optional<RefreshTokenEntity>

    fun findByUserId(userId: UUID): List<RefreshTokenEntity>

    @Query("SELECT rt FROM RefreshTokenEntity rt WHERE rt.userId = :userId AND rt.revokedAt IS NULL AND rt.expiresAt > :now")
    fun findActiveTokensByUserId(@Param("userId") userId: UUID, @Param("now") now: Instant = Instant.now()): List<RefreshTokenEntity>

    @Modifying
    @Query("UPDATE RefreshTokenEntity rt SET rt.revokedAt = :now, rt.lastModifiedAt = :now WHERE rt.userId = :userId AND rt.revokedAt IS NULL")
    fun revokeAllUserTokens(@Param("userId") userId: UUID, @Param("now") now: Instant = Instant.now()): Int

    @Modifying
    @Query("DELETE FROM RefreshTokenEntity rt WHERE rt.expiresAt < :cutoffDate OR rt.revokedAt < :cutoffDate")
    fun deleteExpiredAndRevokedTokens(@Param("cutoffDate") cutoffDate: Instant): Int

    fun existsByJti(jti: String): Boolean
}
