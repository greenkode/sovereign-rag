package ai.sovereignrag.knowledgebase.knowledgebase.gateway

import ai.sovereignrag.commons.exception.ProcessServiceException
import ai.sovereignrag.commons.exception.RecordNotFoundException
import ai.sovereignrag.commons.internal.CreateKBOAuthClientRequest
import ai.sovereignrag.commons.internal.CreateKBOAuthClientResponse
import ai.sovereignrag.commons.internal.OrganizationResponse
import ai.sovereignrag.commons.internal.RevokeKBOAuthClientResponse
import ai.sovereignrag.commons.internal.RotateSecretResponse
import ai.sovereignrag.commons.internal.UpdateOrganizationDatabaseRequest
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.util.UUID

private val log = KotlinLogging.logger {}

@Service
class IdentityService(
    @Qualifier("identityMsRestClient") private val restClient: RestClient
) : IdentityServiceGateway {

    override fun createKBOAuthClient(organizationId: UUID, knowledgeBaseId: String, name: String): KBOAuthCredentials {
        log.info { "Creating KB OAuth client via identity-ms for KB: $knowledgeBaseId" }

        val request = CreateKBOAuthClientRequest(
            organizationId = organizationId,
            knowledgeBaseId = knowledgeBaseId,
            name = name
        )

        val response = restClient.post()
            .uri("/internal/kb-oauth-clients")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .body(CreateKBOAuthClientResponse::class.java)
            ?: throw ProcessServiceException("Failed to create KB OAuth client: empty response")

        if (!response.success) {
            throw ProcessServiceException("Failed to create KB OAuth client: ${response.message}")
        }

        log.info { "KB OAuth client created successfully: ${response.clientId}" }

        return KBOAuthCredentials(
            clientId = response.clientId ?: throw ProcessServiceException("Missing clientId in response"),
            clientSecret = response.clientSecret ?: throw ProcessServiceException("Missing clientSecret in response"),
            knowledgeBaseId = response.knowledgeBaseId ?: knowledgeBaseId
        )
    }

    override fun revokeKBOAuthClient(knowledgeBaseId: String) {
        log.info { "Revoking KB OAuth client via identity-ms for KB: $knowledgeBaseId" }

        val response = restClient.delete()
            .uri("/internal/kb-oauth-clients/{knowledgeBaseId}", knowledgeBaseId)
            .retrieve()
            .body(RevokeKBOAuthClientResponse::class.java)
            ?: throw ProcessServiceException("Failed to revoke KB OAuth client: empty response")

        if (!response.success) {
            throw ProcessServiceException("Failed to revoke KB OAuth client: ${response.message}")
        }

        log.info { "KB OAuth client revoked successfully for KB: $knowledgeBaseId" }
    }

    override fun rotateKBOAuthClientSecret(knowledgeBaseId: String): KBOAuthCredentials {
        log.info { "Rotating KB OAuth client secret via identity-ms for KB: $knowledgeBaseId" }

        val response = restClient.post()
            .uri("/internal/kb-oauth-clients/{knowledgeBaseId}/rotate-secret", knowledgeBaseId)
            .retrieve()
            .body(RotateSecretResponse::class.java)
            ?: throw ProcessServiceException("Failed to rotate KB OAuth client secret: empty response")

        if (!response.success) {
            throw ProcessServiceException("Failed to rotate KB OAuth client secret: ${response.message}")
        }

        log.info { "KB OAuth client secret rotated successfully: ${response.clientId}" }

        return KBOAuthCredentials(
            clientId = response.clientId ?: throw ProcessServiceException("Missing clientId in response"),
            clientSecret = response.clientSecret ?: throw ProcessServiceException("Missing clientSecret in response"),
            knowledgeBaseId = knowledgeBaseId
        )
    }

    override fun getOrganization(organizationId: UUID): OrganizationInfo {
        log.debug { "Fetching organization info from identity-ms: $organizationId" }

        val response = restClient.get()
            .uri("/internal/organizations/{organizationId}", organizationId)
            .retrieve()
            .body(OrganizationResponse::class.java)
            ?: throw RecordNotFoundException("Organization not found: $organizationId")

        return OrganizationInfo(
            id = response.id,
            name = response.name,
            slug = response.slug,
            databaseName = response.databaseName,
            databaseCreated = response.databaseCreated,
            maxKnowledgeBases = response.maxKnowledgeBases
        )
    }

    override fun updateOrganizationDatabase(organizationId: UUID, databaseName: String) {
        log.info { "Updating organization database in identity-ms: $organizationId -> $databaseName" }

        restClient.patch()
            .uri("/internal/organizations/{organizationId}/database", organizationId)
            .contentType(MediaType.APPLICATION_JSON)
            .body(UpdateOrganizationDatabaseRequest(databaseName = databaseName))
            .retrieve()
            .toBodilessEntity()

        log.info { "Organization database updated successfully" }
    }
}
