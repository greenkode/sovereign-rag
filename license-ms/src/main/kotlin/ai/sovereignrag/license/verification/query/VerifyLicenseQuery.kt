package ai.sovereignrag.license.verification.query

import ai.sovereignrag.license.domain.LicenseFeature
import ai.sovereignrag.license.domain.LicenseTier
import java.time.Instant

data class VerifyLicenseQuery(
    val licenseKey: String,
    val deploymentId: String?,
    val hostname: String?,
    val applicationVersion: String?,
    val ipAddress: String?,
    val metadata: Map<String, Any>?
)

data class VerifyLicenseResult(
    val valid: Boolean,
    val message: String?,
    val clientId: String,
    val clientName: String,
    val tier: LicenseTier,
    val maxTokensPerMonth: Long,
    val maxTenants: Int,
    val features: Set<LicenseFeature>,
    val issuedAt: Instant,
    val expiresAt: Instant?,
    val revoked: Boolean
)
