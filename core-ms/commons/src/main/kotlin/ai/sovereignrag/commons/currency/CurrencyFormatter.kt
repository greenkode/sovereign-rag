package ai.sovereignrag.commons.currency

import javax.money.CurrencyUnit
import javax.money.format.MonetaryFormats
import java.util.Locale

object CurrencyFormatter {
    fun getSymbol(currency: CurrencyUnit, locale: Locale = Locale.getDefault()): String {
        // Try using JavaMoney's formatting capabilities
        return try {
            val formatter = MonetaryFormats.getAmountFormat(locale)
            // This is a bit of a hack - format a zero amount and extract just the symbol
            val formatted = formatter.format(org.javamoney.moneta.Money.of(0, currency))
            // Extract just the symbol (this is simplified and may need adjustment)
            formatted.replace(Regex("[0-9.,\\s]+"), "").trim()
        } catch (e: Exception) {
            // Fall back to our map
            val symbolMap = mapOf(
                "NGN" to "₦",
                "USD" to "$",
                "EUR" to "€",
                "GBP" to "£",
                "JPY" to "¥",
                "INR" to "₹"
            )
            symbolMap[currency.currencyCode] ?: currency.currencyCode
        }
    }
}