package ai.sovereignrag.commons.integration.payload.request

import ai.sovereignrag.commons.process.ProcessDto
import javax.money.MonetaryAmount

data class ArrearsSettlementRequest(val amount: MonetaryAmount, val accountNumber: String, val process: ProcessDto)
