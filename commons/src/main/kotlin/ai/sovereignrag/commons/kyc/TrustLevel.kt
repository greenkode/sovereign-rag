package ai.sovereignrag.commons.kyc

enum class TrustLevel(val description: String, val level: Int) {

    TIER_ZERO("Tier Zero", 0),
    TIER_ONE("Tier One", 1),
    TIER_TWO("Tier Two", 2),
    TIER_THREE("Tier Three", 3)
}