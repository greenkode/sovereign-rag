package ai.sovereignrag.commons.license

import java.time.Instant

data class LicenseInfo(
    val licenseKey: String,
    val customerId: String,
    val customerName: String,
    val tier: LicenseTier,
    val maxTokensPerMonth: Long,
    val maxTenants: Int,
    val features: Set<LicenseFeature>,
    val issuedAt: Instant,
    val expiresAt: Instant?,
    val isValid: Boolean,
    val validationMessage: String? = null
) {
    fun isExpired(): Boolean {
        return expiresAt?.let { it.isBefore(Instant.now()) } ?: false
    }

    fun hasFeature(feature: LicenseFeature): Boolean {
        return features.contains(feature)
    }

    fun isTrial(): Boolean {
        return tier == LicenseTier.TRIAL
    }
}

enum class LicenseTier {
    TRIAL,
    STARTER,
    PROFESSIONAL,
    ENTERPRISE,
    UNLIMITED
}

enum class LicenseFeature {
    MULTI_TENANT,
    READ_REPLICAS,
    CUSTOM_MODELS,
    ADVANCED_GUARDRAILS,
    PRIORITY_SUPPORT,
    CUSTOM_INTEGRATIONS,
    WHITE_LABEL,
    ON_PREMISE,
    HIGH_AVAILABILITY,
    ADVANCED_ANALYTICS
}
