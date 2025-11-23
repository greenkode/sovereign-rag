package ai.sovereignrag.pricing.domain

import ai.sovereignrag.commons.exception.PricingServiceException
import ai.sovereignrag.commons.pricing.dto.PricingCalculation
import ai.sovereignrag.pricing.domain.calculation.Calculation
import ai.sovereignrag.pricing.domain.calculation.CustomMaxPercentageCalculation
import ai.sovereignrag.pricing.domain.calculation.FixedCalculation
import ai.sovereignrag.pricing.domain.calculation.FixedRangeCalculation
import ai.sovereignrag.pricing.domain.calculation.MaxPercentageCalculation
import ai.sovereignrag.pricing.domain.calculation.MinValueFixedCalculation
import ai.sovereignrag.pricing.domain.calculation.PercentageCalculation


object PricingCalculationFactory {
    fun getCalculator(calculation: PricingCalculation): Calculation {

        return when (calculation) {
            PricingCalculation.FIXED -> FixedCalculation()
            PricingCalculation.PERCENTAGE -> PercentageCalculation()
            PricingCalculation.MAX_PERCENTAGE -> MaxPercentageCalculation()
            PricingCalculation.MIN_VALUE_FIXED -> MinValueFixedCalculation()
            PricingCalculation.CUSTOM_MAX_PERCENTAGE -> CustomMaxPercentageCalculation()
            PricingCalculation.FIXED_RANGE -> FixedRangeCalculation()
            else -> throw PricingServiceException("Unable to find configured pricing. Please contact our customer service.")
        }
    }
}
