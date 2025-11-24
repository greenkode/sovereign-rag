package ai.sovereignrag.accounting.gateway.dto

import javax.money.MonetaryAmount

data class CompositeAccountBalanceDto(val id: String, val balance: MonetaryAmount)