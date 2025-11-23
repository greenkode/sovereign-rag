package ai.sovereignrag.commons.accounting

import java.util.UUID
import javax.money.CurrencyUnit

interface AccountAddressGateway {

    fun findByAccountAndIntegrationIdAndCurrency(accountId: UUID, exchangeId: String, currency: CurrencyUnit)

}