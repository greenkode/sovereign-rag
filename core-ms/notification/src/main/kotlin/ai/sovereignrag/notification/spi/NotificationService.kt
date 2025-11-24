package ai.sovereignrag.notification.spi

import ai.sovereignrag.commons.exception.RecordNotFoundException
import ai.sovereignrag.commons.integration.model.IntegrationConfigAction
import ai.sovereignrag.commons.json.ObjectMapperFacade
import ai.sovereignrag.commons.json.writeValueAsString
import ai.sovereignrag.commons.notification.NotificationGateway
import ai.sovereignrag.commons.notification.SendMessageResult
import ai.sovereignrag.commons.notification.dto.MessageRecipient
import ai.sovereignrag.commons.notification.enumeration.MessageChannel
import ai.sovereignrag.commons.notification.enumeration.MessagePriority
import ai.sovereignrag.commons.notification.enumeration.TemplateName
import ai.sovereignrag.commons.notification.enumeration.TemplateRecipientType
import ai.sovereignrag.notification.service.MessagingService
import io.github.oshai.kotlinlogging.KotlinLogging.logger
import org.springframework.stereotype.Service
import java.util.Locale

@Service
class NotificationService(private val messagingService: MessagingService) :
    NotificationGateway {

    private val log = logger {}

    override fun sendNotification(
        recipient: MessageRecipient,
        templateName: TemplateName,
        channel: MessageChannel,
        priority: MessagePriority,
        parameters: Map<String, Any>,
        locale: Locale,
        clientIdentifier: String,
        recipientType: TemplateRecipientType
    ): SendMessageResult {

        val template = messagingService.findTemplateByNameAndLocaleAndChannelAndRecipientType(
            templateName, locale,
            channel, recipientType
        ) ?: throw RecordNotFoundException("Unable to find message template for name: ${templateName}")

        log.info { "Using system user for email sending" }

        return messagingService.sendMessage(
            recipient,
            template.toDto(),
            channel,
            priority,
            clientIdentifier,
            parameters,
            IntegrationConfigAction.SEND_EMAIL,
            ObjectMapperFacade.writeValueAsString(parameters)
        )
    }

    override fun sendNotificationWithCc(
        recipients: List<MessageRecipient>,
        templateName: TemplateName,
        channel: MessageChannel,
        priority: MessagePriority,
        parameters: Map<String, Any>,
        locale: Locale,
        clientIdentifier: String,
        recipientType: TemplateRecipientType
    ): SendMessageResult {

        val template = messagingService.findTemplateByNameAndLocaleAndChannelAndRecipientType(
            templateName, locale,
            channel, recipientType
        ) ?: throw RecordNotFoundException("Unable to find message template for name: $templateName")

        log.info { "Using system user for CC email sending to ${recipients.size} recipients" }

        return messagingService.sendMessageWithCc(
            recipients,
            template.toDto(),
            channel,
            priority,
            clientIdentifier,
            parameters,
            IntegrationConfigAction.SEND_EMAIL,
            ObjectMapperFacade.writeValueAsString(parameters)
        )
    }
}