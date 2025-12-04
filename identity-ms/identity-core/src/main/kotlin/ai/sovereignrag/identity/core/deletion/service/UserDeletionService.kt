package ai.sovereignrag.identity.core.deletion.service

import ai.sovereignrag.identity.core.entity.OAuthUser
import ai.sovereignrag.identity.core.refreshtoken.domain.RefreshTokenRepository
import ai.sovereignrag.identity.core.repository.OAuthProviderAccountRepository
import ai.sovereignrag.identity.core.repository.OAuthUserRepository
import ai.sovereignrag.identity.core.trusteddevice.domain.TrustedDeviceRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

private val log = KotlinLogging.logger {}

@Service
class UserDeletionService(
    private val userRepository: OAuthUserRepository,
    private val providerAccountRepository: OAuthProviderAccountRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val trustedDeviceRepository: TrustedDeviceRepository
) {

    @Transactional
    fun deleteUser(userId: UUID): UserDeletionResult {
        val user = userRepository.findById(userId).orElse(null)
            ?: return UserDeletionResult(
                success = false,
                message = "error.user_not_found",
                userId = userId
            )

        log.info { "Starting GDPR-compliant deletion for user: $userId" }

        revokeAllUserTokens(userId)
        deleteAllTrustedDevices(userId)
        anonymizeProviderAccounts(userId)
        anonymizeAndDisableUser(user)

        log.info { "Completed GDPR-compliant deletion for user: $userId" }

        return UserDeletionResult(
            success = true,
            message = "user.deleted_successfully",
            userId = userId
        )
    }

    @Transactional
    fun deleteUsersByOrganizationId(organizationId: UUID): Int {
        val users = userRepository.findByOrganizationId(organizationId)
        log.info { "Deleting ${users.size} users for organization: $organizationId" }

        users.forEach { user ->
            user.id?.let { userId ->
                revokeAllUserTokens(userId)
                deleteAllTrustedDevices(userId)
                anonymizeProviderAccounts(userId)
                anonymizeAndDisableUser(user)
            }
        }

        return users.size
    }

    private fun revokeAllUserTokens(userId: UUID) {
        val revokedCount = refreshTokenRepository.revokeAllUserTokens(userId, Instant.now())
        log.debug { "Revoked $revokedCount refresh tokens for user: $userId" }
    }

    private fun deleteAllTrustedDevices(userId: UUID) {
        trustedDeviceRepository.deleteAllByUserId(userId)
        log.debug { "Deleted all trusted devices for user: $userId" }
    }

    private fun anonymizeProviderAccounts(userId: UUID) {
        val anonymizedCount = providerAccountRepository.anonymizeByUserId(
            userId = userId,
            anonymizedEmail = ANONYMIZED_EMAIL,
            anonymizedId = "${ANONYMIZED_PROVIDER_ID}_${userId.toString().take(8)}"
        )
        log.debug { "Anonymized $anonymizedCount provider accounts for user: $userId" }
    }

    private fun anonymizeAndDisableUser(user: OAuthUser) {
        val anonymizedSuffix = user.id?.toString()?.take(8) ?: UUID.randomUUID().toString().take(8)

        user.username = "${ANONYMIZED_PREFIX}_$anonymizedSuffix"
        user.email = "${ANONYMIZED_PREFIX}_$anonymizedSuffix@deleted.local"
        user.password = ""
        user.firstName = null
        user.middleName = null
        user.lastName = null
        user.phoneNumber = null
        user.pictureUrl = null
        user.dateOfBirth = null
        user.taxIdentificationNumber = null
        user.enabled = false
        user.accountNonExpired = false
        user.accountNonLocked = false
        user.credentialsNonExpired = false
        user.authorities.clear()

        userRepository.save(user)
        log.debug { "Anonymized and disabled user: ${user.id}" }
    }

    companion object {
        private const val ANONYMIZED_PREFIX = "deleted_user"
        private const val ANONYMIZED_EMAIL = "deleted@anonymized.local"
        private const val ANONYMIZED_PROVIDER_ID = "anonymized"
    }
}

data class UserDeletionResult(
    val success: Boolean,
    val message: String,
    val userId: UUID
)
