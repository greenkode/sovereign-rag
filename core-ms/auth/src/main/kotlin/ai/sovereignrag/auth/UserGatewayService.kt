package ai.sovereignrag.auth

import ai.sovereignrag.auth.identity.IdentityMsIntegration
import ai.sovereignrag.commons.kyc.TrustLevel
import ai.sovereignrag.commons.user.UserGateway
import ai.sovereignrag.commons.user.dto.CreateExternalIdPayload
import ai.sovereignrag.commons.user.dto.CreateUserPayload
import ai.sovereignrag.commons.user.dto.MerchantDetailsDto
import ai.sovereignrag.commons.user.dto.UpdateMerchantEnvironmentResult
import ai.sovereignrag.commons.user.dto.UpdateUserPayload
import ai.sovereignrag.commons.user.dto.UserDetailsDto
import ai.sovereignrag.commons.user.dto.UserType
import io.github.oshai.kotlinlogging.KotlinLogging.logger
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.Locale
import java.util.UUID

@Service
class UserGatewayService(
    private val identityMsIntegration: IdentityMsIntegration
) : UserGateway {

    private val log = logger {}

    companion object {
        private val SYSTEM_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001")
        private val SYSTEM_MERCHANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000002")

        private const val USER_ID_CLAIM = "user_id"
        private const val MERCHANT_ID_CLAIM = "merchant_id"
    }

    override fun getLoggedInUserId(): UUID? {
        val jwt = getJwtFromSecurityContext()
        return jwt?.getClaimAsString(USER_ID_CLAIM)?.let { UUID.fromString(it) }
            ?: jwt?.subject?.runCatching { UUID.fromString(this) }?.getOrNull()
    }

    fun getLoggedInMerchantId(): UUID? {
        return getJwtFromSecurityContext()?.getClaimAsString(MERCHANT_ID_CLAIM)?.let { UUID.fromString(it) }
    }

    override fun getSystemUserId(): UUID = SYSTEM_USER_ID

    override fun getSystemMerchantId(): UUID = SYSTEM_MERCHANT_ID

    override fun getLoggedInUserDetails(): UserDetailsDto? {
        val userId = getLoggedInUserId() ?: return null
        return getUserDetailsById(userId)
    }

    override fun getUserDetailsById(id: UUID): UserDetailsDto? {
        return identityMsIntegration.getUserInfo(id)?.let { mapToUserDetailsDto(it) }
    }

    override fun saveExternalId(createExternalIdPayload: CreateExternalIdPayload) {
        identityMsIntegration.saveExternalId(createExternalIdPayload)
    }

    override fun createUser(createUserPayload: CreateUserPayload) {
        identityMsIntegration.createUser(createUserPayload)
    }

    override fun updateUser(updateUserPayload: UpdateUserPayload) {
        identityMsIntegration.updateUser(updateUserPayload)
    }

    override fun getAuthenticatedUserClaims(): Map<String, Any> {
        val authentication = SecurityContextHolder.getContext().authentication as? JwtAuthenticationToken
            ?: return emptyMap()
        val jwt = authentication.credentials as? Jwt ?: return emptyMap()
        return jwt.claims
    }

    override fun authorizeAction(userId: UUID, pin: String): Boolean = true

    override fun getLoggedInMerchantDetails(): MerchantDetailsDto? {
        val merchantId = getLoggedInMerchantId() ?: return null
        return getMerchantDetailsById(merchantId)
    }

    override fun getMerchantDetailsById(merchantId: UUID): MerchantDetailsDto? {
        return identityMsIntegration.getMerchantInfo(merchantId)?.let { mapToMerchantDetailsDto(it, merchantId) }
    }

    override fun updateMerchantEnvironment(merchantId: String, environmentMode: String): UpdateMerchantEnvironmentResult {
        val response = identityMsIntegration.updateMerchantEnvironment(merchantId, environmentMode)
            ?: return UpdateMerchantEnvironmentResult(
                merchantId = merchantId,
                environmentMode = environmentMode,
                updatedAt = Instant.now(),
                affectedUsers = 0
            )

        return UpdateMerchantEnvironmentResult(
            merchantId = response["merchantId"]?.toString() ?: merchantId,
            environmentMode = response["environmentMode"]?.toString() ?: environmentMode,
            updatedAt = response["updatedAt"]?.toString()?.let { Instant.parse(it) } ?: Instant.now(),
            affectedUsers = (response["affectedUsers"] as? Number)?.toInt() ?: 0
        )
    }

    private fun getJwtFromSecurityContext(): Jwt? {
        val jwtAuthenticationToken = SecurityContextHolder.getContext().authentication as? JwtAuthenticationToken
        return jwtAuthenticationToken?.principal as? Jwt
    }

    private fun mapToUserDetailsDto(data: Map<String, Any>): UserDetailsDto? {
        return runCatching {
            UserDetailsDto(
                publicId = UUID.fromString(data["userId"]?.toString() ?: data["id"]?.toString()),
                merchantId = data["merchantId"]?.toString()?.let { UUID.fromString(it) },
                trustLevel = data["trustLevel"]?.toString()?.let { TrustLevel.valueOf(it) } ?: TrustLevel.TIER_ZERO,
                type = data["type"]?.toString()?.let { UserType.valueOf(it) } ?: UserType.INDIVIDUAL,
                firstName = data["firstName"]?.toString(),
                lastName = data["lastName"]?.toString(),
                locale = data["locale"]?.toString()?.let { Locale.forLanguageTag(it) } ?: Locale.getDefault(),
                taxIdentificationNumber = data["taxIdentificationNumber"]?.toString()
            )
        }.onFailure { log.error(it) { "Error mapping user details response" } }
            .getOrNull()
    }

    private fun mapToMerchantDetailsDto(data: Map<String, Any>, fallbackId: UUID): MerchantDetailsDto? {
        return runCatching {
            MerchantDetailsDto(
                id = data["merchantId"]?.toString()?.let { UUID.fromString(it) }
                    ?: data["id"]?.toString()?.let { UUID.fromString(it) }
                    ?: fallbackId,
                name = data["name"]?.toString() ?: "",
                email = data["email"]?.toString() ?: "",
                phoneNumber = data["phoneNumber"]?.toString(),
                locale = data["locale"]?.toString()?.let { Locale.forLanguageTag(it) } ?: Locale.getDefault()
            )
        }.onFailure { log.error(it) { "Error mapping merchant details response" } }
            .getOrNull()
    }
}
