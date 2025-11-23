package ai.sovereignrag.pricing.domain.calculation

import ai.sovereignrag.commons.exception.InvalidRequestException
import ai.sovereignrag.commons.exception.RecordNotFoundException
import ai.sovereignrag.commons.json.ObjectMapperFacade
import com.fasterxml.jackson.core.type.TypeReference
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class FixedRangeCalculation : Calculation {
    override fun calculate(context: CalculationContext): BigDecimal {

        val typeRef = object : TypeReference<List<RangePricingFormula>>() {
        }

        val values = ObjectMapperFacade.fromJson(context.data.expression ?: throw InvalidRequestException("{pricing.data.not.found}"), typeRef)

        val pricingFormula: RangePricingFormula = values.firstOrNull { v -> isBetween(context.amount, v.min, v.max) }
            ?: throw RecordNotFoundException("Unable to find pricing for Transaction. Contact support.")

        return pricingFormula.fee
    }

    companion object {
        fun <T : Comparable<T>?> isBetween(value: T, start: T, end: T): Boolean {
            return value!! >= start && value <= end
        }
    }
}
