package ai.sovereignrag.commons.user.dto

import java.time.Instant

data class UpdateMerchantEnvironmentResult(
    val merchantId: String,
    val environmentMode: String,
    val updatedAt: Instant,
    val affectedUsers: Int
)
