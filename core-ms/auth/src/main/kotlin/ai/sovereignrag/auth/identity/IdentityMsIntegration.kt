package ai.sovereignrag.auth.identity

import ai.sovereignrag.commons.user.dto.CreateExternalIdPayload
import ai.sovereignrag.commons.user.dto.CreateUserPayload
import ai.sovereignrag.commons.user.dto.UpdateUserPayload
import io.github.oshai.kotlinlogging.KotlinLogging.logger
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.util.UUID

@Component
class IdentityMsIntegration(
    @Qualifier("identityMsRestClient") private val restClient: RestClient
) {

    private val log = logger {}

    fun getUserInfo(userId: UUID): Map<String, Any>? {
        return runCatching {
            restClient.get()
                .uri("/api/internal/users/{userId}", userId.toString())
                .retrieve()
                .body(object : ParameterizedTypeReference<Map<String, Any>>() {})
                ?.takeIf { !it.containsKey("error") }
        }.onFailure { log.error(it) { "Failed to fetch user info for userId: $userId" } }
            .getOrNull()
    }

    fun getMerchantInfo(merchantId: UUID): Map<String, Any>? {
        return runCatching {
            restClient.get()
                .uri("/api/internal/merchants/{merchantId}", merchantId.toString())
                .retrieve()
                .body(object : ParameterizedTypeReference<Map<String, Any>>() {})
                ?.takeIf { !it.containsKey("error") }
        }.onFailure { log.error(it) { "Failed to fetch merchant info for merchantId: $merchantId" } }
            .getOrNull()
    }

    fun createUser(payload: CreateUserPayload): Map<String, Any>? {
        return runCatching {
            restClient.post()
                .uri("/api/internal/users")
                .body(payload)
                .retrieve()
                .body(object : ParameterizedTypeReference<Map<String, Any>>() {})
        }.onFailure { log.error(it) { "Failed to create user: ${payload.userId}" } }
            .getOrNull()
    }

    fun updateUser(payload: UpdateUserPayload): Map<String, Any>? {
        return runCatching {
            restClient.put()
                .uri("/api/internal/users/{userId}", payload.userId.toString())
                .body(payload)
                .retrieve()
                .body(object : ParameterizedTypeReference<Map<String, Any>>() {})
        }.onFailure { log.error(it) { "Failed to update user: ${payload.userId}" } }
            .getOrNull()
    }

    fun saveExternalId(payload: CreateExternalIdPayload): Map<String, Any>? {
        return runCatching {
            restClient.post()
                .uri("/api/internal/users/{userId}/external-ids", payload.userId.toString())
                .body(payload)
                .retrieve()
                .body(object : ParameterizedTypeReference<Map<String, Any>>() {})
        }.onFailure { log.error(it) { "Failed to save external id for user: ${payload.userId}" } }
            .getOrNull()
    }

    fun updateMerchantEnvironment(merchantId: String, environmentMode: String): Map<String, Any>? {
        return runCatching {
            restClient.put()
                .uri("/api/internal/merchants/{merchantId}/environment", merchantId)
                .body(mapOf("environmentMode" to environmentMode))
                .retrieve()
                .body(object : ParameterizedTypeReference<Map<String, Any>>() {})
        }.onFailure { log.error(it) { "Failed to update merchant environment for merchantId: $merchantId" } }
            .getOrNull()
    }
}
