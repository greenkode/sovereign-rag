package ai.sovereignrag.commons.currency

import javax.money.CurrencyContext
import javax.money.CurrencyUnit

class CustomCurrencyUnit(
    private val currencyCode: String,
    private val numericCode: Int,
    private val defaultFractionDigits: Int,
    private val currencySymbol: String,
    private val currencyContext: CurrencyContext
) : CurrencyUnit, java.io.Serializable {

    override fun getCurrencyCode(): String = currencyCode
    
    override fun getNumericCode(): Int = numericCode
    
    override fun getDefaultFractionDigits(): Int = defaultFractionDigits
    
    override fun getContext(): CurrencyContext = currencyContext
    
    override fun toString(): String = currencyCode
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CurrencyUnit) return false
        return currencyCode == other.currencyCode
    }
    
    override fun hashCode(): Int = currencyCode.hashCode()

    override fun compareTo(other: CurrencyUnit?): Int {
        return other?.let { currencyCode.compareTo(it.currencyCode) }
            ?: throw IllegalArgumentException("Cannot compare to null currency")
    }
}