package ai.sovereignrag.license.service

import ai.sovereignrag.license.domain.License
import ai.sovereignrag.license.domain.LicenseStatus
import ai.sovereignrag.license.repository.LicenseRepository
import ai.sovereignrag.license.repository.LicenseVerificationRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant

private val log = KotlinLogging.logger {}

@Service
class LicenseVerificationService(
    private val licenseRepository: LicenseRepository,
    private val verificationRepository: LicenseVerificationRepository
) {

    @Transactional
    fun verifyLicense(request: VerificationRequest): VerificationResponse {
        val licenseKeyHash = hashLicenseKey(request.licenseKey)

        log.info { "Verifying license for deployment: ${request.deploymentId}" }

        val license = licenseRepository.findByLicenseKey(request.licenseKey)
            .takeIf { it != null }
            ?: return recordFailure(
                licenseKeyHash,
                request,
                "License not found"
            )

        if (license.status == LicenseStatus.REVOKED) {
            return recordFailure(
                licenseKeyHash,
                request,
                "License has been revoked: ${license.revocationReason}"
            )
        }

        if (license.status == LicenseStatus.SUSPENDED) {
            return recordFailure(
                licenseKeyHash,
                request,
                "License is suspended"
            )
        }

        if (license.expiresAt?.let { it.isBefore(Instant.now()) } == true) {
            return recordFailure(
                licenseKeyHash,
                request,
                "License has expired"
            )
        }

        return recordSuccess(license, licenseKeyHash, request)
    }

    @Transactional
    fun reportUsage(request: UsageReportRequest) {
        log.info { "Recording usage for deployment: ${request.deploymentId}" }

        val licenseKeyHash = hashLicenseKey(request.licenseKey)

        verificationRepository.recordUsage(
            licenseKeyHash = licenseKeyHash,
            clientId = request.clientId,
            deploymentId = request.deploymentId,
            tokensUsed = request.tokensUsed,
            activeKnowledgeBases = request.activeKnowledgeBases,
            activeUsers = request.activeUsers,
            apiCalls = request.apiCalls,
            metadata = request.metadata
        )

        log.info { "Usage recorded successfully" }
    }

    private fun recordSuccess(
        license: License,
        licenseKeyHash: String,
        request: VerificationRequest
    ): VerificationResponse {
        verificationRepository.recordVerification(
            licenseKeyHash = licenseKeyHash,
            clientId = license.clientId,
            deploymentId = request.deploymentId,
            ipAddress = request.ipAddress,
            hostname = request.hostname,
            applicationVersion = request.applicationVersion,
            success = true,
            failureReason = null,
            metadata = request.metadata
        )

        return VerificationResponse(
            valid = true,
            clientId = license.clientId,
            clientName = null,
            tier = license.tier.name,
            maxTokensPerMonth = license.maxTokensPerMonth,
            maxKnowledgeBases = license.maxKnowledgeBases,
            features = license.features.toList(),
            expiresAt = license.expiresAt,
            message = "License is valid"
        )
    }

    private fun recordFailure(
        licenseKeyHash: String,
        request: VerificationRequest,
        reason: String
    ): VerificationResponse {
        verificationRepository.recordVerification(
            licenseKeyHash = licenseKeyHash,
            clientId = "unknown",
            deploymentId = request.deploymentId,
            ipAddress = request.ipAddress,
            hostname = request.hostname,
            applicationVersion = request.applicationVersion,
            success = false,
            failureReason = reason,
            metadata = request.metadata
        )

        return VerificationResponse(
            valid = false,
            message = reason
        )
    }

    private fun hashLicenseKey(licenseKey: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(licenseKey.toByteArray(StandardCharsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}

data class VerificationRequest(
    val licenseKey: String,
    val deploymentId: String?,
    val ipAddress: String?,
    val hostname: String?,
    val applicationVersion: String?,
    val metadata: Map<String, Any>?
)

data class VerificationResponse(
    val valid: Boolean,
    val clientId: String? = null,
    val clientName: String? = null,
    val tier: String? = null,
    val maxTokensPerMonth: Long? = null,
    val maxKnowledgeBases: Int? = null,
    val features: List<String>? = null,
    val expiresAt: Instant? = null,
    val message: String
)

data class UsageReportRequest(
    val licenseKey: String,
    val clientId: String,
    val deploymentId: String?,
    val tokensUsed: Long,
    val activeKnowledgeBases: Int,
    val activeUsers: Int,
    val apiCalls: Long,
    val metadata: Map<String, Any>?
)
