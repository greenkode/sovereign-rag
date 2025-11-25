package ai.sovereignrag.identity.core.event

import ai.sovereignrag.identity.commons.audit.AuditEvent
import ai.sovereignrag.identity.commons.audit.AuditPayloadKey.ACTOR_ID
import ai.sovereignrag.identity.commons.audit.AuditPayloadKey.ACTOR_NAME
import ai.sovereignrag.identity.commons.audit.AuditPayloadKey.EVENT
import ai.sovereignrag.identity.commons.audit.AuditPayloadKey.EVENT_TIME
import ai.sovereignrag.identity.commons.audit.AuditPayloadKey.IDENTITY_TYPE
import ai.sovereignrag.identity.commons.audit.AuditPayloadKey.MERCHANT_ID
import ai.sovereignrag.identity.commons.audit.AuditPayloadKey.PAYLOAD
import ai.sovereignrag.identity.commons.audit.AuditPayloadKey.RESOURCE
import io.github.oshai.kotlinlogging.KotlinLogging
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
        runCatching {
            log.debug { "Sending audit event to Audit MS: ${event.event} for ${event.actorId}" }

            val requestBody = mapOf(
                ACTOR_ID.value to event.actorId,
                ACTOR_NAME.value to event.actorName,
                MERCHANT_ID.value to event.merchantId,
                IDENTITY_TYPE.value to event.identityType.name,
                RESOURCE.value to event.resource.name,
                EVENT.value to event.event,
                EVENT_TIME.value to event.eventTime,
                PAYLOAD.value to event.payload
            )

            RestClient.builder()
                .baseUrl(auditMsBaseUrl)
                .build()
                .post()
                .uri("/v1/events/create")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(requestBody)
                .retrieve()
                .body(Map::class.java)
                ?.let { log.info { "Audit event sent successfully to Audit MS: ${it["message"]}" } }
        }.onFailure { e ->
            log.error(e) { "Failed to send audit event to Audit MS: ${event.event}" }
        }
    }
}