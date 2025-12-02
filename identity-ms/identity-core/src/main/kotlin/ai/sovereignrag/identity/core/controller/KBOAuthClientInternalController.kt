package ai.sovereignrag.identity.core.controller

import ai.sovereignrag.identity.core.entity.OAuthRegisteredClient
import ai.sovereignrag.identity.core.entity.OrganizationStatus
import ai.sovereignrag.identity.core.repository.OAuthRegisteredClientRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.ResponseEntity
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

data class CreateKBOAuthClientRequest(
    val organizationId: UUID,
    val knowledgeBaseId: String,
    val name: String
)

data class CreateKBOAuthClientResponse(
    val success: Boolean,
    val clientId: String? = null,
    val clientSecret: String? = null,
    val knowledgeBaseId: String? = null,
    val message: String? = null
)

data class RevokeResponse(
    val success: Boolean,
    val message: String? = null
)

data class RotateSecretResponse(
    val success: Boolean,
    val clientId: String? = null,
    val clientSecret: String? = null,
    val message: String? = null
)

@RestController
@RequestMapping("/internal/kb-oauth-clients")
class KBOAuthClientInternalController(
    private val oauthRegisteredClientRepository: OAuthRegisteredClientRepository,
    private val passwordEncoder: PasswordEncoder
) {

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_internal')")
    @Transactional
    fun createKBOAuthClient(@RequestBody request: CreateKBOAuthClientRequest): ResponseEntity<CreateKBOAuthClientResponse> {
        log.info { "Creating KB OAuth client for KB: ${request.knowledgeBaseId}, Org: ${request.organizationId}" }

        val existing = oauthRegisteredClientRepository.findByKnowledgeBaseIdAndStatus(
            request.knowledgeBaseId,
            OrganizationStatus.ACTIVE
        )

        if (existing != null) {
            log.warn { "KB OAuth client already exists for KB: ${request.knowledgeBaseId}" }
            return ResponseEntity.badRequest().body(
                CreateKBOAuthClientResponse(
                    success = false,
                    message = "OAuth client already exists for this knowledge base"
                )
            )
        }

        val clientId = "kb_${request.knowledgeBaseId.replace("-", "").take(16)}_${generateRandomString(8)}"
        val clientSecret = generateSecureClientSecret()
        val encodedSecret = passwordEncoder.encode(clientSecret)

        val client = OAuthRegisteredClient().apply {
            id = UUID.randomUUID().toString()
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

        return ResponseEntity.ok(
            CreateKBOAuthClientResponse(
                success = true,
                clientId = clientId,
                clientSecret = clientSecret,
                knowledgeBaseId = request.knowledgeBaseId
            )
        )
    }

    @DeleteMapping("/{knowledgeBaseId}")
    @PreAuthorize("hasAuthority('SCOPE_internal')")
    @Transactional
    fun revokeKBOAuthClient(@PathVariable knowledgeBaseId: String): ResponseEntity<RevokeResponse> {
        log.info { "Revoking KB OAuth client for KB: $knowledgeBaseId" }

        val client = oauthRegisteredClientRepository.findByKnowledgeBaseIdAndStatus(
            knowledgeBaseId,
            OrganizationStatus.ACTIVE
        )

        if (client == null) {
            log.warn { "KB OAuth client not found for KB: $knowledgeBaseId" }
            return ResponseEntity.notFound().build()
        }

        client.status = OrganizationStatus.DELETED
        oauthRegisteredClientRepository.save(client)

        log.info { "KB OAuth client revoked successfully for KB: $knowledgeBaseId" }

        return ResponseEntity.ok(RevokeResponse(success = true, message = "OAuth client revoked successfully"))
    }

    @PostMapping("/{knowledgeBaseId}/rotate-secret")
    @PreAuthorize("hasAuthority('SCOPE_internal')")
    @Transactional
    fun rotateKBOAuthClientSecret(@PathVariable knowledgeBaseId: String): ResponseEntity<RotateSecretResponse> {
        log.info { "Rotating KB OAuth client secret for KB: $knowledgeBaseId" }

        val client = oauthRegisteredClientRepository.findByKnowledgeBaseIdAndStatus(
            knowledgeBaseId,
            OrganizationStatus.ACTIVE
        )

        if (client == null) {
            log.warn { "KB OAuth client not found for KB: $knowledgeBaseId" }
            return ResponseEntity.notFound().build()
        }

        val newSecret = generateSecureClientSecret()
        client.clientSecret = passwordEncoder.encode(newSecret)
        oauthRegisteredClientRepository.save(client)

        log.info { "KB OAuth client secret rotated successfully for KB: $knowledgeBaseId" }

        return ResponseEntity.ok(
            RotateSecretResponse(
                success = true,
                clientId = client.clientId,
                clientSecret = newSecret
            )
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
