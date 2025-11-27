package ai.sovereignrag.notification.core.service

import ai.sovereignrag.commons.notification.dto.MessageRecipient
import ai.sovereignrag.commons.notification.dto.SendNotificationRequest
import ai.sovereignrag.commons.notification.dto.SendNotificationResponse
import ai.sovereignrag.commons.notification.enumeration.DeliveryStatus
import ai.sovereignrag.notification.core.entity.MessageEntity
import ai.sovereignrag.notification.core.repository.MessageRepository
import ai.sovereignrag.notification.core.repository.MessageTemplateRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

private val log = KotlinLogging.logger {}

@Service
@Transactional
class NotificationService(
    private val messageTemplateRepository: MessageTemplateRepository,
    private val messageRepository: MessageRepository,
    private val emailSender: EmailSender
) {

    fun sendNotification(request: SendNotificationRequest): SendNotificationResponse {
        log.info { "Processing notification request: template=${request.templateName}, channel=${request.channel}" }

        val template = messageTemplateRepository.findByNameAndChannelAndLocaleAndRecipientTypeAndActiveIsTrue(
            request.templateName,
            request.channel,
            request.locale,
            request.recipientType
        ) ?: run {
            log.warn { "Template not found: ${request.templateName}" }
            return SendNotificationResponse(
                status = DeliveryStatus.FAILED,
                messageId = UUID.randomUUID().toString(),
                message = "Template not found: ${request.templateName}"
            )
        }

        val messageId = UUID.randomUUID().toString()
        val primaryRecipient = request.recipients.firstOrNull()
            ?: return SendNotificationResponse(
                status = DeliveryStatus.FAILED,
                messageId = messageId,
                message = "No recipients provided"
            )

        val notificationRequest = request
        val message = MessageEntity().apply {
            publicId = UUID.fromString(messageId)
            channel = notificationRequest.channel
            this.template = template
            templateName = notificationRequest.templateName
            recipient = primaryRecipient.address
            recipientName = primaryRecipient.name
            deliveryStatus = DeliveryStatus.PENDING
            this.request = notificationRequest.parameters.toString()
            priority = notificationRequest.priority
            clientIdentifier = notificationRequest.clientIdentifier
            locale = notificationRequest.locale
            createdAt = Instant.now()
        }

        messageRepository.save(message)

        return runCatching {
            emailSender.sendEmail(
                recipients = request.recipients,
                template = template,
                parameters = request.parameters
            )

            message.deliveryStatus = DeliveryStatus.SENT
            message.deliveryDate = Instant.now()
            messageRepository.save(message)

            log.info { "Notification sent successfully: messageId=$messageId" }

            SendNotificationResponse(
                status = DeliveryStatus.SENT,
                messageId = messageId,
                message = "Notification sent successfully"
            )
        }.getOrElse { e ->
            log.error(e) { "Failed to send notification: messageId=$messageId" }
            message.deliveryStatus = DeliveryStatus.FAILED
            message.response = e.message
            messageRepository.save(message)

            SendNotificationResponse(
                status = DeliveryStatus.FAILED,
                messageId = messageId,
                message = "Failed to send notification: ${e.message}"
            )
        }
    }
}

interface EmailSender {
    fun sendEmail(
        recipients: List<MessageRecipient>,
        template: ai.sovereignrag.notification.core.entity.MessageTemplateEntity,
        parameters: Map<String, String>
    )
}
