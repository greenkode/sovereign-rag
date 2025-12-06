package ai.sovereignrag.identity.core.controller

import ai.sovereignrag.commons.exception.InvalidRequestException
import ai.sovereignrag.commons.exception.RecordNotFoundException
import ai.sovereignrag.commons.internal.CreateKBOAuthClientRequest
import ai.sovereignrag.commons.internal.CreateKBOAuthClientResponse
import ai.sovereignrag.commons.internal.RevokeKBOAuthClientResponse
import ai.sovereignrag.commons.internal.RotateSecretResponse
import ai.sovereignrag.identity.core.entity.OAuthRegisteredClient
import ai.sovereignrag.identity.core.entity.OrganizationStatus
import ai.sovereignrag.identity.core.repository.OAuthRegisteredClientRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import java.util.UUID

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/internal/kb-oauth-clients")
class KBOAuthClientInternalController(
    private val oauthRegisteredClientRepository: OAuthRegisteredClientRepository,
    private val passwordEncoder: PasswordEncoder
) {

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_internal')")
    @Transactional
    fun createKBOAuthClient(@RequestBody request: CreateKBOAuthClientRequest): CreateKBOAuthClientResponse {
        log.info { "Creating KB OAuth client for KB: ${request.knowledgeBaseId}, Org: ${request.organizationId}" }

        oauthRegisteredClientRepository.findByKnowledgeBaseIdAndStatus(
            request.knowledgeBaseId,
            OrganizationStatus.ACTIVE
        )?.let {
            log.warn { "KB OAuth client already exists for KB: ${request.knowledgeBaseId}" }
            throw InvalidRequestException("OAuth client already exists for this knowledge base")
        }

        val clientId = "kb_${request.knowledgeBaseId.replace("-", "").take(16)}_${generateRandomString(8)}"
        val clientSecret = generateSecureClientSecret()
        val encodedSecret = passwordEncoder.encode(clientSecret)

        val client = OAuthRegisteredClient().apply {
            this.clientId = clientId
            this.clientIdIssuedAt = Instant.now()
            this.clientSecret = encodedSecret
            this.clientName = request.name
            this.status = OrganizationStatus.ACTIVE
            this.organizationId = request.organizationId
            this.knowledgeBaseId = request.knowledgeBaseId
        }

        oauthRegisteredClientRepository.save(client)

        log.info { "KB OAuth client created successfully: $clientId for KB: ${request.knowledgeBaseId}" }

        return CreateKBOAuthClientResponse(
            success = true,
            clientId = clientId,
            clientSecret = clientSecret,
            knowledgeBaseId = request.knowledgeBaseId
        )
    }

    @DeleteMapping("/{knowledgeBaseId}")
    @PreAuthorize("hasAuthority('SCOPE_internal')")
    @Transactional
    fun revokeKBOAuthClient(@PathVariable knowledgeBaseId: String): RevokeKBOAuthClientResponse {
        log.info { "Revoking KB OAuth client for KB: $knowledgeBaseId" }

        val client = oauthRegisteredClientRepository.findByKnowledgeBaseIdAndStatus(
            knowledgeBaseId,
            OrganizationStatus.ACTIVE
        ) ?: run {
            log.warn { "KB OAuth client not found for KB: $knowledgeBaseId" }
            throw RecordNotFoundException("KB OAuth client not found for KB: $knowledgeBaseId")
        }

        client.status = OrganizationStatus.DELETED
        oauthRegisteredClientRepository.save(client)

        log.info { "KB OAuth client revoked successfully for KB: $knowledgeBaseId" }

        return RevokeKBOAuthClientResponse(success = true, message = "OAuth client revoked successfully")
    }

    @PostMapping("/{knowledgeBaseId}/rotate-secret")
    @PreAuthorize("hasAuthority('SCOPE_internal')")
    @Transactional
    fun rotateKBOAuthClientSecret(@PathVariable knowledgeBaseId: String): RotateSecretResponse {
        log.info { "Rotating KB OAuth client secret for KB: $knowledgeBaseId" }

        val client = oauthRegisteredClientRepository.findByKnowledgeBaseIdAndStatus(
            knowledgeBaseId,
            OrganizationStatus.ACTIVE
        ) ?: run {
            log.warn { "KB OAuth client not found for KB: $knowledgeBaseId" }
            throw RecordNotFoundException("KB OAuth client not found for KB: $knowledgeBaseId")
        }

        val newSecret = generateSecureClientSecret()
        client.clientSecret = passwordEncoder.encode(newSecret)
        oauthRegisteredClientRepository.save(client)

        log.info { "KB OAuth client secret rotated successfully for KB: $knowledgeBaseId" }

        return RotateSecretResponse(
            success = true,
            clientId = client.clientId,
            clientSecret = newSecret
        )
    }

    private fun generateRandomString(length: Int): String {
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
        return (1..length).map { chars.random() }.joinToString("")
    }

    private fun generateSecureClientSecret(): String {
        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
