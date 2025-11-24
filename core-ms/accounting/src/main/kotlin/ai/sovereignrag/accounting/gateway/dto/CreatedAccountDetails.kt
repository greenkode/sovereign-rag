package ai.sovereignrag.accounting.gateway.dto

import java.util.UUID
import javax.money.CurrencyUnit
import javax.money.MonetaryAmount

data class CreatedAccountDetails(val id: UUID, val number: String, val currency: CurrencyUnit, val balance: MonetaryAmount)
