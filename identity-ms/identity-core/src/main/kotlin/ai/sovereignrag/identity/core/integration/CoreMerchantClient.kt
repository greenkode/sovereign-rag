package ai.sovereignrag.identity.core.integration

import ai.sovereignrag.identity.commons.notification.MessagePayload
import ai.sovereignrag.identity.commons.notification.toNotificationRequest
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient


@Service
class CoreMerchantClient(
    @Qualifier("oauth2RestClient")
    private val restClient: RestClient
) {
    private val log = KotlinLogging.logger {}

    fun sendMessage(messagePayload: MessagePayload) {

        log.info { "Sending message via CoreMS notification endpoint with OAuth2 authentication" }
        
        val notificationRequest = messagePayload.toNotificationRequest()
        
        try {
            val response = restClient.post()
                .uri("/notifications/send")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(notificationRequest)
                .retrieve()
                .body(String::class.java)
            
            log.info { "Message sent successfully: $response" }
        } catch (e: Exception) {
            log.error(e) { "Failed to send message via CoreMS" }
            throw e
        }
    }
}
