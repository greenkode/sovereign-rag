package ai.sovereignrag.accounting.gateway.api.response

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder(
    "chart"
)
class CreateCurrencyChartRequest(
    @JsonProperty("chart") var chart: String,
    @JsonIgnore private val additionalProperties: MutableMap<String, Any> = LinkedHashMap()
)