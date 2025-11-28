package ai.sovereignrag.identity.core.refreshtoken.service

import ai.sovereignrag.identity.commons.exception.ClientException
import ai.sovereignrag.identity.commons.i18n.MessageService
import ai.sovereignrag.identity.core.auth.service.JwtTokenService
import ai.sovereignrag.identity.core.entity.OAuthUser
import ai.sovereignrag.identity.core.refreshtoken.domain.RefreshTokenEntity
import ai.sovereignrag.identity.core.refreshtoken.domain.RefreshTokenRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.time.Instant
import java.util.HexFormat
import java.util.UUID

private val log = KotlinLogging.logger {}

@Service
@Transactional
class RefreshTokenService(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtTokenService: JwtTokenService,
    private val messageService: MessageService
) {

    private fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(token.toByteArray())
        return HexFormat.of().formatHex(hashBytes)
    }

    fun createRefreshToken(
        user: OAuthUser,
        ipAddress: String? = null,
        userAgent: String? = null,
        deviceFingerprint: String? = null
    ): String {
        val jti = UUID.randomUUID().toString()
        val token = jwtTokenService.generateRefreshToken(user)
        val tokenHash = hashToken(token)

        val refreshToken = RefreshTokenEntity(
            jti = jti,
            userId = user.id!!,
            tokenHash = tokenHash,
            ipAddress = ipAddress,
            userAgent = userAgent,
            deviceFingerprint = deviceFingerprint,
            issuedAt = Instant.now(),
            expiresAt = Instant.now().plusSeconds(jwtTokenService.getRefreshTokenExpirySeconds())
        )

        refreshTokenRepository.save(refreshToken)
        log.info { "Refresh token created for user ${user.username} with jti: $jti" }

        return token
    }

    fun validateAndGetRefreshToken(token: String, jti: String): RefreshTokenEntity {
        val refreshToken = refreshTokenRepository.findByJti(jti)
            .orElseThrow { ClientException(messageService.getMessage("auth.error.invalid_refresh_token")) }

        if (!refreshToken.isValid()) {
            if (refreshToken.isRevoked()) {
                log.warn { "Attempt to use revoked refresh token: $jti" }
                throw ClientException(messageService.getMessage("auth.error.refresh_token_revoked"))
            }
            if (refreshToken.isExpired()) {
                log.warn { "Attempt to use expired refresh token: $jti" }
                throw ClientException(messageService.getMessage("auth.error.refresh_token_expired"))
            }
        }

        val computedHash = hashToken(token)
        if (computedHash != refreshToken.tokenHash) {
            log.warn { "Invalid refresh token hash for jti: $jti" }
            throw ClientException(messageService.getMessage("auth.error.invalid_refresh_token"))
        }

        return refreshToken
    }

    fun validateAndGetRefreshTokenByHash(token: String): RefreshTokenEntity {
        val tokenHash = hashToken(token)
        val refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
            .orElseThrow { ClientException(messageService.getMessage("auth.error.invalid_refresh_token")) }

        if (refreshToken.isRevoked()) {
            log.warn { "Attempt to use revoked refresh token: ${refreshToken.jti}" }
            throw ClientException(messageService.getMessage("auth.error.refresh_token_revoked"))
        }

        if (refreshToken.isExpired()) {
            log.warn { "Attempt to use expired refresh token: ${refreshToken.jti}" }
            throw ClientException(messageService.getMessage("auth.error.refresh_token_expired"))
        }

        return refreshToken
    }

    fun rotateRefreshToken(
        oldToken: RefreshTokenEntity,
        user: OAuthUser,
        ipAddress: String? = null,
        userAgent: String? = null,
        deviceFingerprint: String? = null
    ): String {
        val newJti = UUID.randomUUID().toString()
        val newToken = jwtTokenService.generateRefreshToken(user)
        val newTokenHash = hashToken(newToken)

        oldToken.revoke(replacedBy = newJti)
        refreshTokenRepository.save(oldToken)

        val refreshToken = RefreshTokenEntity(
            jti = newJti,
            userId = user.id!!,
            tokenHash = newTokenHash,
            ipAddress = ipAddress ?: oldToken.ipAddress,
            userAgent = userAgent ?: oldToken.userAgent,
            deviceFingerprint = deviceFingerprint ?: oldToken.deviceFingerprint,
            issuedAt = Instant.now(),
            expiresAt = Instant.now().plusSeconds(jwtTokenService.getRefreshTokenExpirySeconds())
        )

        refreshTokenRepository.save(refreshToken)
        log.info { "Refresh token rotated for user ${user.username}. Old jti: ${oldToken.jti}, New jti: $newJti" }

        return newToken
    }

    fun revokeToken(jti: String) {
        val refreshToken = refreshTokenRepository.findByJti(jti)
            .orElseThrow { ClientException(messageService.getMessage("auth.error.refresh_token_not_found")) }

        refreshToken.revoke()
        refreshTokenRepository.save(refreshToken)
        log.info { "Refresh token revoked: $jti" }
    }

    fun revokeRefreshTokenByHash(token: String) {
        val tokenHash = hashToken(token)
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent { refreshToken ->
            refreshToken.revoke()
            refreshTokenRepository.save(refreshToken)
            log.info { "Refresh token revoked by hash: ${refreshToken.jti}" }
        }
    }

    fun revokeAllUserTokens(userId: UUID): Int {
        val revokedCount = refreshTokenRepository.revokeAllUserTokens(userId)
        log.info { "Revoked $revokedCount refresh tokens for user $userId" }
        return revokedCount
    }

    fun getActiveTokensForUser(userId: UUID): List<RefreshTokenEntity> {
        return refreshTokenRepository.findActiveTokensByUserId(userId)
    }

    fun cleanupExpiredTokens(daysOld: Long = 30): Int {
        val cutoffDate = Instant.now().minusSeconds(daysOld * 86400)
        val deletedCount = refreshTokenRepository.deleteExpiredAndRevokedTokens(cutoffDate)
        log.info { "Cleaned up $deletedCount expired/revoked refresh tokens older than $daysOld days" }
        return deletedCount
    }
}
