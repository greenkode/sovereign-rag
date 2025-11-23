package ai.sovereignrag.pricing.domain.calculation

import ai.sovereignrag.commons.json.ObjectMapperFacade
import com.fasterxml.jackson.core.type.TypeReference
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class MinValueFixedCalculation : Calculation {
    override fun calculate(context: CalculationContext): BigDecimal {
        val typeRef: TypeReference<Map<PricingFormulaKeys, BigDecimal>> =
            object : TypeReference<Map<PricingFormulaKeys, BigDecimal>>() {
            }

        val values = ObjectMapperFacade.fromJson(context.data.expression, typeRef)

        val min = values.getOrDefault(PricingFormulaKeys.MIN, BigDecimal.ZERO)

        return if (context.amount >= min) context.data.value else BigDecimal.ZERO
    }
}
