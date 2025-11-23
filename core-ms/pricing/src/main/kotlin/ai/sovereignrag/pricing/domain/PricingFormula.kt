package ai.sovereignrag.pricing.domain

import java.math.BigDecimal

data class PricingFormula(val name: String, val min: BigDecimal, val max: BigDecimal, val value: BigDecimal)
