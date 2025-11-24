package ai.sovereignrag.accounting.gateway.api.response

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import java.util.UUID


@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder(
    "id", "number", "currency", "balance"
)
data class CreateAccountResponse(
    @JsonProperty("id") val id: UUID,
    @JsonProperty("number") val number: String,
    @JsonProperty("currency") val currency: String,
    @JsonProperty("balance") val balance: MoneyResponse,
    @JsonIgnore val additionalProperties: MutableMap<String, Any> = LinkedHashMap()
)
