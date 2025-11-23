package ai.sovereignrag.pricing.domain.calculation

import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
interface Calculation {
    fun calculate(context: CalculationContext): BigDecimal
}
