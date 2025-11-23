package ai.sovereignrag.commons.accounting

import java.util.UUID

data class PayoutTransferInitiatedEvent(
    val processId: UUID,
    val requestId: Long,
    val exchangeReference: String,
    val exchangeId: String,
)