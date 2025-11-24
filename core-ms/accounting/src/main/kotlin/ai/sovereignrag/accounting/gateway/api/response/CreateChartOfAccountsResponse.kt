package ai.sovereignrag.accounting.gateway.api.response

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder(
    "message"
)
class CreateChartOfAccountsResponse(
    @JsonProperty("message") var message: String,
    @JsonIgnore private val additionalProperties: MutableMap<String, Any> = LinkedHashMap()
)