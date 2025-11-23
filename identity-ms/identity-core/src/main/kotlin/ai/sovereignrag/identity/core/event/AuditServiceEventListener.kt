package ai.sovereignrag.identity.core.event

import ai.sovereignrag.identity.commons.audit.AuditEvent
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.event.EventListener
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class AuditServiceEventListener(
    @Value("\${audit-ms.base-url}") private val auditMsBaseUrl: String
) {
    private val log = KotlinLogging.logger {}

    @Async
    @EventListener
    fun on(event: AuditEvent) {
        try {
            log.debug { "Sending audit event to Audit MS: ${event.event} for ${event.actorId}" }

            val requestBody = mapOf(
                "actorId" to event.actorId,
                "actorName" to event.actorName,
                "merchantId" to event.merchantId,
                "identityType" to event.identityType.name,
                "resource" to event.resource.name,
                "event" to event.event,
                "eventTime" to event.eventTime,
                "payload" to event.payload
            )

            val restClient = RestClient.builder()
                .baseUrl(auditMsBaseUrl)
                .build()

            val response = restClient.post()
                .uri("/v1/events/create")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(requestBody)
                .retrieve()
                .body(Map::class.java)

            log.info { "Audit event sent successfully to Audit MS: ${response?.get("message")}" }
        } catch (e: Exception) {
            log.error(e) { "Failed to send audit event to Audit MS: ${event.event}" }
        }
    }
}