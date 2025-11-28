package ai.sovereignrag.commons.license

import ai.sovereignrag.commons.subscription.SubscriptionTier
import java.time.Instant

data class LicenseInfo(
    val licenseKey: String,
    val customerId: String,
    val customerName: String,
    val tier: SubscriptionTier,
    val maxTokensPerMonth: Long,
    val maxTenants: Int,
    val features: Set<LicenseFeature>,
    val issuedAt: Instant,
    val expiresAt: Instant?,
    val isValid: Boolean,
    val validationMessage: String? = null
) {
    fun isExpired(): Boolean {
        return expiresAt?.isBefore(Instant.now()) ?: false
    }

    fun hasFeature(feature: LicenseFeature): Boolean {
        return features.contains(feature)
    }

    fun isTrial(): Boolean {
        return tier == SubscriptionTier.TRIAL
    }
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
