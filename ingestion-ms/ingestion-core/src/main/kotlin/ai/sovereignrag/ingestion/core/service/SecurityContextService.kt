package ai.sovereignrag.ingestion.core.service

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SecurityContextService {

    companion object {
        private const val MERCHANT_ID_CLAIM = "merchant_id"
        private const val USER_ID_CLAIM = "user_id"
    }

    fun getCurrentMerchantId(): UUID {
        val jwt = getJwtFromSecurityContext()
            ?: throw IllegalStateException("No JWT authentication found")

        val merchantIdClaim = jwt.getClaimAsString(MERCHANT_ID_CLAIM)
            ?: throw IllegalStateException("Merchant ID not found in JWT claims")

        return UUID.fromString(merchantIdClaim)
    }

    fun getCurrentUserId(): UUID {
        val jwt = getJwtFromSecurityContext()
            ?: throw IllegalStateException("No JWT authentication found")

        val userIdClaim = jwt.getClaimAsString(USER_ID_CLAIM)
            ?: jwt.subject

        return UUID.fromString(userIdClaim)
    }

    fun getCurrentMerchantIdOrNull(): UUID? {
        return getJwtFromSecurityContext()
            ?.getClaimAsString(MERCHANT_ID_CLAIM)
            ?.let { UUID.fromString(it) }
    }

    fun getCurrentUserIdOrNull(): UUID? {
        val jwt = getJwtFromSecurityContext() ?: return null
        val userIdClaim = jwt.getClaimAsString(USER_ID_CLAIM) ?: jwt.subject
        return userIdClaim?.let { UUID.fromString(it) }
    }

    private fun getJwtFromSecurityContext(): Jwt? {
        val authentication = SecurityContextHolder.getContext().authentication as? JwtAuthenticationToken
        return authentication?.principal as? Jwt
    }
}
