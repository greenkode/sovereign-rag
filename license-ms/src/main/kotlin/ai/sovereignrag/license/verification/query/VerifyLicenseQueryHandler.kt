package ai.sovereignrag.license.verification.query

import ai.sovereignrag.license.config.MessageService
import ai.sovereignrag.license.domain.LicenseStatus
import ai.sovereignrag.license.repository.LicenseRepository
import ai.sovereignrag.license.repository.LicenseVerificationRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64

private val log = KotlinLogging.logger {}

@Service
class VerifyLicenseQueryHandler(
    private val licenseRepository: LicenseRepository,
    private val verificationRepository: LicenseVerificationRepository,
    private val messageService: MessageService
) {

    @Transactional
    fun handle(query: VerifyLicenseQuery): VerifyLicenseResult {
        val licenseKeyHash = hashLicenseKey(query.licenseKey)

        val license = licenseRepository.findByLicenseKey(query.licenseKey)
            ?: return handleLicenseNotFound(licenseKeyHash, query)

        val isExpired = license.expiresAt?.let { it.isBefore(Instant.now()) } ?: false
        val isRevoked = license.status == LicenseStatus.REVOKED
        val isSuspended = license.status == LicenseStatus.SUSPENDED
        val isValid = !isExpired && !isRevoked && !isSuspended

        val failureReason = when {
            isRevoked -> messageService.getMessage("license.error.revoked")
            isSuspended -> messageService.getMessage("license.error.suspended")
            isExpired -> messageService.getMessage("license.error.expired")
            else -> null
        }

        verificationRepository.recordVerification(
            licenseKeyHash = licenseKeyHash,
            clientId = license.clientId,
            deploymentId = query.deploymentId,
            ipAddress = query.ipAddress,
            hostname = query.hostname,
            applicationVersion = query.applicationVersion,
            success = isValid,
            failureReason = failureReason,
            metadata = query.metadata
        )

        log.info { "License verification for ${license.clientId}: valid=$isValid" }

        val features = license.features.mapNotNull { featureString ->
            runCatching { ai.sovereignrag.license.domain.LicenseFeature.valueOf(featureString) }.getOrNull()
        }.toSet()

        return VerifyLicenseResult(
            valid = isValid,
            message = failureReason,
            clientId = license.clientId,
            clientName = license.clientId,
            tier = license.tier,
            maxTokensPerMonth = license.maxTokensPerMonth,
            maxTenants = license.maxTenants,
            features = features,
            issuedAt = license.createdAt,
            expiresAt = license.expiresAt,
            revoked = isRevoked
        )
    }

    private fun handleLicenseNotFound(licenseKeyHash: String, query: VerifyLicenseQuery): VerifyLicenseResult {
        val message = messageService.getMessage("license.error.key_not_found")

        verificationRepository.recordVerification(
            licenseKeyHash = licenseKeyHash,
            clientId = "unknown",
            deploymentId = query.deploymentId,
            ipAddress = query.ipAddress,
            hostname = query.hostname,
            applicationVersion = query.applicationVersion,
            success = false,
            failureReason = message,
            metadata = query.metadata
        )

        return VerifyLicenseResult(
            valid = false,
            message = message,
            clientId = "unknown",
            clientName = "Unknown",
            tier = ai.sovereignrag.license.domain.LicenseTier.TRIAL,
            maxTokensPerMonth = 0,
            maxTenants = 0,
            features = emptySet(),
            issuedAt = Instant.now(),
            expiresAt = null,
            revoked = false
        )
    }

    private fun hashLicenseKey(licenseKey: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(licenseKey.toByteArray())
        return Base64.getEncoder().encodeToString(hashBytes)
    }
}
