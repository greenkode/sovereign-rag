package ai.sovereignrag.auth

import ai.sovereignrag.commons.user.UserGateway
import ai.sovereignrag.commons.user.dto.CreateExternalIdPayload
import ai.sovereignrag.commons.user.dto.CreateUserPayload
import ai.sovereignrag.commons.user.dto.MerchantDetailsDto
import ai.sovereignrag.commons.user.dto.UpdateMerchantEnvironmentResult
import ai.sovereignrag.commons.user.dto.UpdateUserPayload
import ai.sovereignrag.commons.user.dto.UserDetailsDto
import io.github.oshai.kotlinlogging.KotlinLogging.logger
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class UserGatewayService : UserGateway {

    private val log = logger {}

    companion object {
        private val SYSTEM_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001")
        private val SYSTEM_MERCHANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000002")

        private const val USER_ID_CLAIM = "user_id"
        private const val MERCHANT_ID_CLAIM = "merchant_id"
    }

    override fun getLoggedInUserId(): UUID? {
        val jwtAuthenticationToken = SecurityContextHolder.getContext().authentication as? JwtAuthenticationToken
        val jwt = jwtAuthenticationToken?.principal as? Jwt
        return jwt?.getClaimAsString(USER_ID_CLAIM)?.let { UUID.fromString(it) }
            ?: jwt?.subject?.runCatching { UUID.fromString(this) }?.getOrNull()
    }

    fun getLoggedInMerchantId(): UUID? {
        val jwtAuthenticationToken = SecurityContextHolder.getContext().authentication as? JwtAuthenticationToken
        val jwt = jwtAuthenticationToken?.principal as? Jwt
        return jwt?.getClaimAsString(MERCHANT_ID_CLAIM)?.let { UUID.fromString(it) }
    }

    override fun getSystemUserId(): UUID = SYSTEM_USER_ID

    override fun getSystemMerchantId(): UUID = SYSTEM_MERCHANT_ID

    override fun getLoggedInUserDetails(): UserDetailsDto? {
        val authentication = SecurityContextHolder.getContext().authentication as? JwtAuthenticationToken ?: return null
        val jwt = authentication.credentials as? Jwt ?: return null
        val userId = jwt.getClaimAsString(USER_ID_CLAIM)?.let { UUID.fromString(it) } ?: return null
        return getUserDetailsById(userId)
    }

    override fun getUserDetailsById(id: UUID): UserDetailsDto? {
        return null
    }

    override fun saveExternalId(createExternalIdPayload: CreateExternalIdPayload) {
        log.warn { "saveExternalId not implemented" }
    }

    override fun createUser(createUserPayload: CreateUserPayload) {
        log.warn { "createUser not implemented" }
    }

    override fun updateUser(updateUserPayload: UpdateUserPayload) {
        log.warn { "updateUser not implemented" }
    }

    override fun getAuthenticatedUserClaims(): Map<String, Any> {
        val authentication = SecurityContextHolder.getContext().authentication as? JwtAuthenticationToken
            ?: return emptyMap()
        val jwt = authentication.credentials as? Jwt ?: return emptyMap()
        return jwt.claims
    }

    override fun authorizeAction(userId: UUID, pin: String): Boolean = true

    override fun getLoggedInMerchantDetails(): MerchantDetailsDto? {
        val authentication = SecurityContextHolder.getContext().authentication as? JwtAuthenticationToken ?: return null
        val jwt = authentication.credentials as? Jwt ?: return null
        val merchantId = jwt.getClaimAsString(MERCHANT_ID_CLAIM)?.let { UUID.fromString(it) } ?: return null
        return getMerchantDetailsById(merchantId)
    }

    override fun getMerchantDetailsById(merchantId: UUID): MerchantDetailsDto? {
        return null
    }

    override fun updateMerchantEnvironment(merchantId: String, environmentMode: String): UpdateMerchantEnvironmentResult {
        log.warn { "updateMerchantEnvironment not implemented" }
        return UpdateMerchantEnvironmentResult(
            merchantId = merchantId,
            environmentMode = environmentMode,
            updatedAt = Instant.now(),
            affectedUsers = 0
        )
    }
}
