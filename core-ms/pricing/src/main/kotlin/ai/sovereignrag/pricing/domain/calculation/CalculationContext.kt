package ai.sovereignrag.pricing.domain.calculation

import ai.sovereignrag.pricing.domain.PricingData
import java.math.BigDecimal

class CalculationContext(
    val data: PricingData, val amount: BigDecimal
)
