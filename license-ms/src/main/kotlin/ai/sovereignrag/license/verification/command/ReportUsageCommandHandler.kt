package ai.sovereignrag.license.verification.command

import ai.sovereignrag.license.repository.LicenseVerificationRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.util.Base64

private val log = KotlinLogging.logger {}

@Service
class ReportUsageCommandHandler(
    private val verificationRepository: LicenseVerificationRepository
) {

    @Transactional
    fun handle(command: ReportUsageCommand): ReportUsageResult {
        val licenseKeyHash = hashLicenseKey(command.licenseKey)

        verificationRepository.recordUsage(
            licenseKeyHash = licenseKeyHash,
            customerId = command.customerId,
            deploymentId = command.deploymentId,
            tokensUsed = command.tokensUsed,
            activeTenants = command.activeTenants,
            activeUsers = command.activeUsers,
            apiCalls = command.apiCalls,
            metadata = command.metadata
        )

        log.info { "Usage reported for ${command.customerId}: tokens=${command.tokensUsed}, tenants=${command.activeTenants}" }

        return ReportUsageResult(
            success = true,
            message = "Usage reported successfully"
        )
    }

    private fun hashLicenseKey(licenseKey: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(licenseKey.toByteArray())
        return Base64.getEncoder().encodeToString(hashBytes)
    }
}
