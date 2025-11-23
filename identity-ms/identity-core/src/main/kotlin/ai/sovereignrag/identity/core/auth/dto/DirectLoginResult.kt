package ai.sovereignrag.identity.core.auth.dto

import java.time.Instant

data class DirectLoginResult(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val tokenType: String = "Bearer",
    val trustedDeviceId: String? = null,
    val trustedUntil: Instant? = null
)