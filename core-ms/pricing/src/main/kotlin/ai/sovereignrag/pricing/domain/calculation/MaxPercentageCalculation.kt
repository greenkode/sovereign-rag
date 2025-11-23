package ai.sovereignrag.pricing.domain.calculation

import ai.sovereignrag.commons.exception.InvalidRequestException
import ai.sovereignrag.commons.json.ObjectMapperFacade
import com.fasterxml.jackson.core.type.TypeReference
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class MaxPercentageCalculation : Calculation {
    override fun calculate(context: CalculationContext): BigDecimal {

        val typeRef: TypeReference<Map<PricingFormulaKeys, BigDecimal>> =
            object : TypeReference<Map<PricingFormulaKeys, BigDecimal>>() {
            }

        val values = ObjectMapperFacade.fromJson(
            context.data.expression ?: throw InvalidRequestException("{pricing.data.not.found.}"), typeRef
        )

        val percentage = context.amount.multiply(context.data.value)

        val min = values.getOrDefault(PricingFormulaKeys.MIN, BigDecimal.ZERO)

        val max = values.getOrDefault(PricingFormulaKeys.MAX, percentage)

        if (percentage <= min) return min

        if (percentage >= max) return max

        return percentage
    }
}
