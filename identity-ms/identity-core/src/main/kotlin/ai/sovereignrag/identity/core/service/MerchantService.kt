package ai.sovereignrag.identity.core.service

import ai.sovereignrag.identity.core.entity.EnvironmentMode
import ai.sovereignrag.identity.core.repository.OAuthRegisteredClientRepository
import ai.sovereignrag.identity.core.repository.OAuthUserRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

private val log = KotlinLogging.logger {}

data class UpdateMerchantEnvironmentResult(
    val merchantId: String,
    val environmentMode: EnvironmentMode,
    val lastModifiedAt: Instant,
    val affectedUsers: Int
)

@Service
@Transactional
class MerchantService(
    private val clientRepository: OAuthRegisteredClientRepository,
    private val userRepository: OAuthUserRepository
) {

    fun updateMerchantEnvironment(merchantId: UUID, environmentMode: EnvironmentMode): UpdateMerchantEnvironmentResult {

        log.info { "Updating merchant environment mode for merchant: $merchantId to: $environmentMode" }

        val merchant = clientRepository.findById(merchantId)
            .orElseThrow { IllegalArgumentException("Merchant with ID $merchantId not found") }

        merchant.environmentMode = environmentMode
        clientRepository.save(merchant)

        log.info { "Successfully updated merchant $merchantId to $environmentMode mode" }

        val affectedUsers = if (environmentMode == EnvironmentMode.SANDBOX) {
            val users = userRepository.findByMerchantId(merchantId)
            val productionUsers = users.filter { it.environmentPreference == EnvironmentMode.PRODUCTION }

            val now = Instant.now()
            productionUsers.forEach { user ->
                user.environmentPreference = EnvironmentMode.SANDBOX
                user.environmentLastSwitchedAt = now
            }

            userRepository.saveAll(productionUsers)

            if (productionUsers.isNotEmpty()) {
                log.info { "Reset ${productionUsers.size} users to SANDBOX after merchant downgrade to SANDBOX" }
            }

            productionUsers.size
        } else {
            0
        }

        return UpdateMerchantEnvironmentResult(
            merchantId = merchantId.toString(),
            environmentMode = environmentMode,
            lastModifiedAt = Instant.now(),
            affectedUsers = affectedUsers
        )
    }
}
