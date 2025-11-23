package ai.sovereignrag.pricing.domain.calculation

import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode


@Component
class PercentageCalculation : Calculation {
    override fun calculate(context: CalculationContext): BigDecimal {
        return context.data.value.multiply(context.amount)
            .setScale(2, RoundingMode.UP)
    }
}
