
package ai.sovereignrag.commons.currency

import javax.money.CurrencyContext
import javax.money.CurrencyContextBuilder
import javax.money.CurrencyQuery
import javax.money.CurrencyUnit
import javax.money.spi.CurrencyProviderSpi
import java.util.*

class CustomCurrencyProvider : CurrencyProviderSpi {
    private val currencies = mutableMapOf<String, CurrencyUnit>()
    private val context = CurrencyContextBuilder.of("CustomProvider").build()

    init {
        // Add your custom NGN with the symbol you want
        registerCurrency("NGN", 566, 2, "₦")
        // Add other currencies as needed
    }

    private fun registerCurrency(code: String, numericCode: Int, fractionDigits: Int, symbol: String) {
        currencies[code] = CustomCurrencyUnit(code, numericCode, fractionDigits, symbol, context)
    }

    override fun getCurrencies(query: CurrencyQuery): Set<CurrencyUnit> {
        val result = HashSet<CurrencyUnit>()
        
        if (query.currencyCodes.isNotEmpty()) {
            for (code in query.currencyCodes) {
                currencies[code]?.let { result.add(it) }
            }
        } else {
            result.addAll(currencies.values)
        }
        
        return result
    }

    override fun getProviderName(): String = "custom"
}