package ai.sovereignrag.accounting.gateway.api.response

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder


@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder(
    "id", "number"
)
data class ChartDetailsResponse(
    @JsonProperty("id") val id: String,
    @JsonProperty("number") val number: String,
    @JsonIgnore val additionalProperties: MutableMap<String, Any> = LinkedHashMap()
)
