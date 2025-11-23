package ai.sovereignrag.pricing.domain.calculation

import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class FixedCalculation : Calculation {
    override fun calculate(context: CalculationContext): BigDecimal {
        return context.data.value
    }
}
