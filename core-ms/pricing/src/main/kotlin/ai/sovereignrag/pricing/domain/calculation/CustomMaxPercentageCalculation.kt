package ai.sovereignrag.pricing.domain.calculation

import ai.sovereignrag.commons.exception.PricingServiceException
import ai.sovereignrag.commons.json.ObjectMapperFacade
import ai.sovereignrag.pricing.domain.PricingFormula
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class CustomMaxPercentageCalculation : Calculation {

    override fun calculate(context: CalculationContext): BigDecimal {

        context.data.expression?.let { expression ->

            val pricingFormula = ObjectMapperFacade.fromJson(context.data.expression, PricingFormula::class.java)

            val percentage = context.amount.multiply(pricingFormula.value)

            if (percentage <= pricingFormula.min) return pricingFormula.min

            if (percentage >= pricingFormula.max) return pricingFormula.max

            return percentage
        }

        throw PricingServiceException(
            "Invalid pricing configuration provided."
        )
    }
}
