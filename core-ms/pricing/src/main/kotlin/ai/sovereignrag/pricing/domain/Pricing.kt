package ai.sovereignrag.pricing.domain

import ai.sovereignrag.commons.accounting.AccountType
import ai.sovereignrag.commons.accounting.TransactionType
import ai.sovereignrag.commons.pricing.dto.PricingCalculation
import ai.sovereignrag.commons.pricing.dto.PricingType
import ai.sovereignrag.pricing.domain.calculation.CalculationContext
import org.javamoney.moneta.Money
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import javax.money.CurrencyUnit
import javax.money.MonetaryAmount

data class Pricing(
    val id: Long, val publicId: UUID, val vendorId: UUID?, val accountType: AccountType?, val accountPublicId: UUID?,
    val transactionType: TransactionType, val currency: CurrencyUnit?, val validFrom: Instant, val validUntil: Instant?,
    val integratorId: String?, val data: Set<PricingData>
) {

    fun getPriceByType(pricingType: PricingType, amount: MonetaryAmount, ): MonetaryAmount {

        return data.filter { it.pricingType == pricingType }
            .map { Money.of(it.calculate(amount), amount.currency) }
            .firstOrNull() ?: Money.zero(amount.currency)
    }
}

data class PricingData(
    val id: Long,
    val pricingType: PricingType,
    val calculation: PricingCalculation,
    val value: BigDecimal,
    val expression: String?
) {

    fun calculate(amount: MonetaryAmount): BigDecimal {
        return PricingCalculationFactory.getCalculator(calculation)
            .calculate(CalculationContext(this, amount.number.numberValue(BigDecimal::class.java)))
    }
}