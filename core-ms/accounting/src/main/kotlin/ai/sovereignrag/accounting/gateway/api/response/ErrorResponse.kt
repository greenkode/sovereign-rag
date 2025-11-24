package ai.sovereignrag.accounting.gateway.api.response

import com.fasterxml.jackson.annotation.JsonProperty

data class ErrorResponse(@JsonProperty("error") val error: String)