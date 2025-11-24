package ai.sovereignrag.commons.integration

import java.math.BigDecimal
import java.time.Instant

data class VendorTransactionDetailsDto(
    val transactionId: String,
    val transactionReference: String,
    val amount: BigDecimal,
    val transactionDate: Instant,
    val description: String?,
    val additionalInfo: Map<String, Any>?
)