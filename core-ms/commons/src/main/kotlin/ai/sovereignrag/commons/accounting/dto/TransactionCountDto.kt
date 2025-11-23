package ai.sovereignrag.commons.accounting.dto

import java.util.UUID

data class TransactionCountDto(
    val merchantId: UUID,
    val merchantName: String,
    val lendingCount: Long,
    val vendingCount: Long,
    val totalCount: Long
)