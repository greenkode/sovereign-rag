package ai.sovereignrag.commons.subscription

enum class SubscriptionTier(
    val displayName: String,
    val monthlyPrice: Double,
    val includedTokens: Long,
    val maxKnowledgeBases: Int,
    val priority: Int
) {
    FREE("Free", 0.0, 100_000L, 1, 0),
    FLY("Fly", 700.0, 10_000_000L, 1, 1),
    GROWTH("Growth", 1750.0, 50_000_000L, 3, 2),
    ENTERPRISE("Enterprise", 5000.0, 500_000_000L, 10, 3);

    fun allowsFeature(feature: String): Boolean {
        return when (this) {
            FREE -> feature in setOf("basic_support", "api_access")
            FLY -> feature in setOf("basic_support", "api_access", "email_support")
            GROWTH -> feature in setOf("basic_support", "api_access", "email_support", "priority_support", "advanced_analytics")
            ENTERPRISE -> true
        }
    }
}
