package ai.sovereignrag.ingestion.core.audit

import ai.sovereignrag.ingestion.commons.audit.IngestionAuditEvent
import ai.sovereignrag.ingestion.commons.audit.IngestionAuditPayloadKey
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.event.EventListener
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.scheduling.annotation.Async
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

private val log = KotlinLogging.logger {}

@Component
class AuditServiceEventListener(
    @Value("\${audit-ms.base-url}") private val auditMsBaseUrl: String,
    private val serviceAuthorizedClientManager: AuthorizedClientServiceOAuth2AuthorizedClientManager
) {

    companion object {
        private const val CLIENT_REGISTRATION_ID = "ingestion-ms-client"
    }

    @Async
    @EventListener
    fun on(event: IngestionAuditEvent) {
        runCatching {
            log.debug { "Sending audit event to Audit MS: ${event.event} for organization ${event.organizationId}" }

            val accessToken = getAccessToken()
                ?: run {
                    log.warn { "Unable to obtain access token for audit-ms communication" }
                    return
                }

            val requestBody = mapOf(
                IngestionAuditPayloadKey.ACTOR_ID.value to event.actorId,
                IngestionAuditPayloadKey.ACTOR_NAME.value to event.actorName,
                IngestionAuditPayloadKey.MERCHANT_ID.value to event.organizationId.toString(),
                IngestionAuditPayloadKey.IDENTITY_TYPE.value to event.identityType.name,
                IngestionAuditPayloadKey.RESOURCE.value to event.resource.name,
                IngestionAuditPayloadKey.EVENT.value to event.event.name,
                IngestionAuditPayloadKey.EVENT_TIME.value to event.eventTime.toString(),
                IngestionAuditPayloadKey.PAYLOAD.value to buildPayload(event)
            )

            RestClient.builder()
                .baseUrl(auditMsBaseUrl)
                .build()
                .post()
                .uri("/v1/events/create")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .body(requestBody)
                .retrieve()
                .body(Map::class.java)
                ?.let { log.info { "Audit event sent successfully to Audit MS: ${it["message"]}" } }
        }.onFailure { e ->
            log.error(e) { "Failed to send audit event to Audit MS: ${event.event}" }
        }
    }

    private fun buildPayload(event: IngestionAuditEvent): Map<String, String> {
        val payload = event.payload.toMutableMap()
        event.jobId?.let { payload[IngestionAuditPayloadKey.JOB_ID.value] = it.toString() }
        event.knowledgeBaseId?.let { payload[IngestionAuditPayloadKey.KNOWLEDGE_BASE_ID.value] = it.toString() }
        return payload
    }

    private fun getAccessToken(): String? =
        runCatching {
            val authorizeRequest = OAuth2AuthorizeRequest
                .withClientRegistrationId(CLIENT_REGISTRATION_ID)
                .principal("ingestion-ms")
                .build()

            serviceAuthorizedClientManager.authorize(authorizeRequest)?.accessToken?.tokenValue
        }.onFailure { e ->
            log.error(e) { "Failed to obtain OAuth2 access token for audit-ms" }
        }.getOrNull()
}
