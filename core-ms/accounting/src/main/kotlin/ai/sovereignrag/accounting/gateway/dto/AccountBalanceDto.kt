package ai.sovereignrag.accounting.gateway.dto

import java.util.UUID
import javax.money.MonetaryAmount

data class AccountBalanceDto(val id: UUID, val balance: MonetaryAmount)