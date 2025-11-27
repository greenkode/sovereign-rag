package ai.sovereignrag.commons.currency

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import javax.money.CurrencyUnit
import javax.money.Monetary

@Converter(autoApply = true)
class CurrencyUnitConverter : AttributeConverter<CurrencyUnit, String> {
    override fun convertToDatabaseColumn(attribute: CurrencyUnit?): String? {
        return attribute?.currencyCode
    }

    override fun convertToEntityAttribute(dbData: String?): CurrencyUnit? {
        return dbData?.let { Monetary.getCurrency(it) }
    }
}
