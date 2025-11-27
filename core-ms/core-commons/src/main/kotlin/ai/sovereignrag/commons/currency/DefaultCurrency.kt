
package ai.sovereignrag.commons.currency

import javax.money.CurrencyUnit
import javax.money.Monetary

object DefaultCurrency {
    const val DEFAULT_CURRENCY_CODE = "NGN"
    
    val currency: CurrencyUnit = Monetary.getCurrency(DEFAULT_CURRENCY_CODE)

    val symbol: String
        get() = CurrencyFormatter.getSymbol(currency)
}