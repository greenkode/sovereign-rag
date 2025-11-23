package ai.sovereignrag.pricing.domain.calculation

import java.math.BigDecimal

data class RangePricingFormula(val fee: BigDecimal, val min: BigDecimal, val max: BigDecimal)
