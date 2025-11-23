package ai.sovereignrag.audit.domain.model

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = true)
class MapToJsonConverter : AttributeConverter<Map<String, Any>, String> {

    private val objectMapper = ObjectMapper()

    override fun convertToDatabaseColumn(attribute: Map<String, Any>?): String {
        return if (attribute.isNullOrEmpty()) {
            "{}"
        } else {
            try {
                objectMapper.writeValueAsString(attribute)
            } catch (e: Exception) {
                "{}"
            }
        }
    }

    override fun convertToEntityAttribute(dbData: String?): Map<String, Any> {
        return if (dbData.isNullOrBlank() || dbData == "{}") {
            emptyMap()
        } else {
            try {
                objectMapper.readValue(dbData, object : TypeReference<Map<String, Any>>() {})
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }
}