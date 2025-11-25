package ai.sovereignrag.identity.commons.dto

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.Instant

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorResponse(
    val error: String,
    val message: String,
    val details: Map<String, String>? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class TokenResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
    val scope: String = "openid email phone profile",
    val user: UserSummary? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class UserSummary(
    val username: String,
    val name: String,
    val email: String
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class UserInfoResponse(
    val sub: String,
    val akuId: String? = null,
    val name: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val username: String? = null,
    val userType: String? = null,
    val trustLevel: String? = null,
    val emailVerified: Boolean? = null,
    val phoneVerified: Boolean? = null,
    val merchantId: String? = null,
    val source: String? = null,
    val requestedBy: String? = null,
    val clientId: String? = null,
    val scopes: List<String>? = null,
    val authenticated: Boolean? = null,
    val authorities: List<String>? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class MerchantInfoResponse(
    val merchantId: String,
    val name: String,
    val email: String?,
    val phone: String? = null,
    val locale: String = "en_US",
    val clientId: String? = null,
    val source: String = "identity-service",
    val requestedBy: String? = null,
    val authenticatedMerchant: Boolean? = null,
    val lowBalanceAlert: Int? = null,
    val failureRate: Int? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class HomeResponse(
    val message: String,
    val user: String,
    val authenticated: Boolean,
    val endpoints: EndpointsInfo
)

data class EndpointsInfo(
    val authorize: String = "/oauth2/authorize",
    val token: String = "/oauth2/token",
    val jwks: String = "/oauth2/jwks",
    val userinfo: String = "/userinfo",
    val openidConfig: String = "/.well-known/openid-configuration"
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class UnlockResponse(
    val status: String,
    val message: String,
    val username: String? = null,
    val clientId: String? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class LockoutStatusResponse(
    val identifier: String,
    val locked: Boolean,
    val remainingMinutes: Long? = null,
    val message: String
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class AccountLockedResponse(
    val error: String = "account_locked",
    val message: String,
    val lockedUntil: String? = null,
    val remainingMinutes: Long
)

@JsonInclude(JsonInclude.Include.NON_NULL)
data class UpdateMerchantEnvironmentResponse(
    val merchantId: String,
    val environmentMode: String,
    val lastModifiedAt: Instant,
    val affectedUsers: Int
)
