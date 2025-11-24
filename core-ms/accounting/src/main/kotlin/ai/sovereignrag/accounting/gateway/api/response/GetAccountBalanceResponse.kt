package ai.sovereignrag.accounting.gateway.api.response

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder(
    "code", "accountId", "balance"
)
data class GetAccountBalanceResponse(
    @JsonProperty("code") val code: String,
    @JsonProperty("accountId") val accountId: UUID,
    @JsonProperty("balance") val balance: MoneyResponse
)