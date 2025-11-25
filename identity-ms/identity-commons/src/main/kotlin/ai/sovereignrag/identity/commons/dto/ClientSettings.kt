package ai.sovereignrag.identity.commons.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class ClientSettings(
    @JsonProperty("emailAddress")
    val emailAddress: String? = null,

    @JsonProperty("phoneNumber")
    val phoneNumber: String? = null,

    @JsonProperty("lowBalance")
    val lowBalance: Int? = null,

    @JsonProperty("failureRate")
    val failureRate: Int? = null
) {
    companion object {
        fun fallback(clientId: String) = ClientSettings(
            emailAddress = "merchant-$clientId@example.com"
        )
    }
}
