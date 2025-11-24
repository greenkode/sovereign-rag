package ai.sovereignrag.accounting.gateway.api.response

import org.javamoney.moneta.Money
import java.math.BigDecimal
import javax.money.MonetaryAmount

data class MoneyResponse(val amount: BigDecimal, val currency: String) {

    fun toMonetaryAmount(): MonetaryAmount {
        return Money.of(amount, currency)
    }
}
