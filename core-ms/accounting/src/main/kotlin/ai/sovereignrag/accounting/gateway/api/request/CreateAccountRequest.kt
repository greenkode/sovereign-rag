package ai.sovereignrag.accounting.gateway.api.request

import ai.sovereignrag.commons.accounting.AccountType
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.UUID

data class CreateAccountRequest(
    @JsonProperty("currency") val currency: String,
    @JsonProperty("name") val description: String,
    @JsonProperty("parent") val parent: String,
    @JsonProperty("padding") val padding: Int,
    @JsonProperty("final") val final: Boolean,
    @JsonProperty("type") val type: String,
    @JsonProperty("metadata") val metadata: Map<String, String>
)