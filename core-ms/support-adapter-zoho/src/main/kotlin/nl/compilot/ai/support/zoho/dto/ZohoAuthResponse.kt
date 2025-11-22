package nl.compilot.ai.support.zoho.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class ZohoAuthResponse(
    @JsonProperty("access_token")
    val accessToken: String,
    @JsonProperty("expires_in")
    val expiresIn: Long,
    @JsonProperty("api_domain")
    val apiDomain: String,
    @JsonProperty("token_type")
    val tokenType: String
)
