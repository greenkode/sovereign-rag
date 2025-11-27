package ai.sovereignrag.identity.core.integration

import ai.sovereignrag.commons.api.ApiResponse
import ai.sovereignrag.commons.notification.dto.MessageRecipient
import ai.sovereignrag.commons.notification.dto.SendNotificationRequest
import ai.sovereignrag.commons.notification.dto.SendNotificationResponse
import ai.sovereignrag.commons.notification.enumeration.MessageChannel
import ai.sovereignrag.commons.notification.enumeration.MessagePriority
import ai.sovereignrag.commons.notification.enumeration.RecipientType
import ai.sovereignrag.commons.notification.enumeration.TemplateName
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.util.Locale

private val log = KotlinLogging.logger {}

@Service
class NotificationClient(
    @Value("\${notification.service.url:http://localhost:8082}")
    private val notificationServiceUrl: String
) {
    private val restClient = RestClient.builder()
        .baseUrl(notificationServiceUrl)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build()

    fun sendNotification(
        recipients: List<MessageRecipient>,
        templateName: TemplateName,
        channel: MessageChannel = MessageChannel.EMAIL,
        priority: MessagePriority = MessagePriority.HIGH,
        parameters: Map<String, String>,
        locale: Locale = Locale.ENGLISH,
        clientIdentifier: String,
        recipientType: RecipientType = RecipientType.INDIVIDUAL
    ): SendNotificationResponse? {
        log.info { "Sending notification via notification-ms: template=$templateName" }

        val request = SendNotificationRequest(
            recipients = recipients,
            templateName = templateName,
            channel = channel,
            priority = priority,
            parameters = parameters,
            locale = locale,
            clientIdentifier = clientIdentifier,
            recipientType = recipientType
        )

        return runCatching {
            val response = restClient.post()
                .uri("/api/v1/notifications/send")
                .body(request)
                .retrieve()
                .body(object : ParameterizedTypeReference<ApiResponse<SendNotificationResponse>>() {})

            log.info { "Notification sent successfully: ${response?.data}" }
            response?.data
        }.getOrElse { e ->
            log.error(e) { "Failed to send notification via notification-ms" }
            null
        }
    }
}
