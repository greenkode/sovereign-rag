package ai.sovereignrag.license.verification.controller

import ai.sovereignrag.license.verification.command.ReportUsageCommand
import ai.sovereignrag.license.verification.command.ReportUsageCommandHandler
import ai.sovereignrag.license.verification.query.VerifyLicenseQuery
import ai.sovereignrag.license.verification.query.VerifyLicenseQueryHandler
import ai.sovereignrag.license.domain.LicenseFeature
import ai.sovereignrag.license.domain.LicenseTier
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/v1/verify")
class LicenseVerificationController(
    private val verifyLicenseQueryHandler: VerifyLicenseQueryHandler,
    private val reportUsageCommandHandler: ReportUsageCommandHandler
) {

    @PostMapping
    fun verifyLicense(
        @RequestBody request: VerifyLicenseApiRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<VerifyLicenseApiResponse> {
        log.info { "License verification request from: ${httpRequest.remoteAddr}" }

        val query = VerifyLicenseQuery(
            licenseKey = request.licenseKey,
            deploymentId = request.deploymentId,
            ipAddress = httpRequest.remoteAddr,
            hostname = request.hostname,
            applicationVersion = request.applicationVersion,
            metadata = request.metadata
        )

        val result = verifyLicenseQueryHandler.handle(query)

        val response = VerifyLicenseApiResponse(
            valid = result.valid,
            message = result.message,
            clientId = result.clientId,
            clientName = result.clientName,
            tier = result.tier,
            maxTokensPerMonth = result.maxTokensPerMonth,
            maxKnowledgeBases = result.maxKnowledgeBases,
            features = result.features,
            issuedAt = result.issuedAt,
            expiresAt = result.expiresAt,
            revoked = result.revoked
        )

        return ResponseEntity.ok(response)
    }

    @PostMapping("/usage")
    fun reportUsage(@RequestBody request: ReportUsageApiRequest): ResponseEntity<ReportUsageApiResponse> {
        log.info { "Usage report from deployment: ${request.deploymentId}" }

        val command = ReportUsageCommand(
            licenseKey = request.licenseKey,
            clientId = request.clientId,
            deploymentId = request.deploymentId,
            tokensUsed = request.tokensUsed,
            activeKnowledgeBases = request.activeKnowledgeBases,
            activeUsers = request.activeUsers,
            apiCalls = request.apiCalls,
            metadata = request.metadata
        )

        val result = reportUsageCommandHandler.handle(command)

        return ResponseEntity.ok(
            ReportUsageApiResponse(
                success = result.success,
                message = result.message
            )
        )
    }
}

data class VerifyLicenseApiRequest(
    val licenseKey: String,
    val deploymentId: String?,
    val hostname: String?,
    val applicationVersion: String?,
    val metadata: Map<String, Any>?
)

data class VerifyLicenseApiResponse(
    val valid: Boolean,
    val message: String?,
    val clientId: String,
    val clientName: String,
    val tier: LicenseTier,
    val maxTokensPerMonth: Long,
    val maxKnowledgeBases: Int,
    val features: Set<LicenseFeature>,
    val issuedAt: Instant,
    val expiresAt: Instant?,
    val revoked: Boolean
)

data class ReportUsageApiRequest(
    val licenseKey: String,
    val clientId: String,
    val deploymentId: String?,
    val tokensUsed: Long,
    val activeKnowledgeBases: Int,
    val activeUsers: Int,
    val apiCalls: Long,
    val metadata: Map<String, Any>?
)

data class ReportUsageApiResponse(
    val success: Boolean,
    val message: String
)
