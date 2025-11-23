package ai.sovereignrag.commons.user.dto

import ai.sovereignrag.commons.user.AccessTokenType
import java.time.Instant

data class AccessTokenDto(
    val type: AccessTokenType, val accessToken: String, val refreshToken: String?,
    val expiry: Instant, val resource: String, val institution: String
) : java.io.Serializable