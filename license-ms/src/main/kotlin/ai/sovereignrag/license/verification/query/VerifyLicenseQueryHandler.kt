package ai.sovereignrag.license.verification.query

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
    private val verificationRepository: LicenseVerificationRepository
) {

    @Transactional
    fun handle(query: VerifyLicenseQuery): VerifyLicenseResult {
        val licenseKeyHash = hashLicenseKey(query.licenseKey)

        val license = licenseRepository.findByLicenseKey(query.licenseKey)

        if (license == null) {
            verificationRepository.recordVerification(
                licenseKeyHash = licenseKeyHash,
                customerId = "unknown",
                deploymentId = query.deploymentId,
                ipAddress = query.ipAddress,
                hostname = query.hostname,
                applicationVersion = query.applicationVersion,
                success = false,
                failureReason = "License not found",
                metadata = query.metadata
            )

            return VerifyLicenseResult(
                valid = false,
                message = "License key not found",
                customerId = "unknown",
                customerName = "Unknown",
                tier = ai.sovereignrag.license.domain.LicenseTier.TRIAL,
                maxTokensPerMonth = 0,
                maxTenants = 0,
                features = emptySet(),
                issuedAt = Instant.now(),
                expiresAt = null,
                revoked = false
            )
        }

        val isExpired = license.expiresAt?.let { it.isBefore(Instant.now()) } ?: false
        val isRevoked = license.status == LicenseStatus.REVOKED
        val isSuspended = license.status == LicenseStatus.SUSPENDED
        val isValid = !isExpired && !isRevoked && !isSuspended

        val failureReason = when {
            isRevoked -> "License revoked"
            isSuspended -> "License suspended"
            isExpired -> "License expired"
            else -> null
        }

        verificationRepository.recordVerification(
            licenseKeyHash = licenseKeyHash,
            customerId = license.customerId,
            deploymentId = query.deploymentId,
            ipAddress = query.ipAddress,
            hostname = query.hostname,
            applicationVersion = query.applicationVersion,
            success = isValid,
            failureReason = failureReason,
            metadata = query.metadata
        )

        log.info { "License verification for ${license.customerId}: valid=$isValid" }

        val features = license.features.mapNotNull { featureString ->
            try {
                ai.sovereignrag.license.domain.LicenseFeature.valueOf(featureString)
            } catch (e: IllegalArgumentException) {
                null
            }
        }.toSet()

        return VerifyLicenseResult(
            valid = isValid,
            message = failureReason,
            customerId = license.customerId,
            customerName = license.customerId,
            tier = license.tier,
            maxTokensPerMonth = license.maxTokensPerMonth,
            maxTenants = license.maxTenants,
            features = features,
            issuedAt = license.createdAt,
            expiresAt = license.expiresAt,
            revoked = isRevoked
        )
    }

    private fun hashLicenseKey(licenseKey: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(licenseKey.toByteArray())
        return Base64.getEncoder().encodeToString(hashBytes)
    }
}
