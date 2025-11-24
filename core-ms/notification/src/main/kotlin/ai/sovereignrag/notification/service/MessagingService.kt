package ai.sovereignrag.notification.service

import ai.sovereignrag.commons.exception.RecordNotFoundException
import ai.sovereignrag.commons.integration.IntegrationConfigGateway
import ai.sovereignrag.commons.integration.model.IntegrationConfigAction
import ai.sovereignrag.commons.json.ObjectMapperFacade
import ai.sovereignrag.commons.json.writeValueAsString
import ai.sovereignrag.commons.notification.SendMessageResult
import ai.sovereignrag.commons.notification.dto.MessageDto
import ai.sovereignrag.commons.notification.dto.MessagePayload
import ai.sovereignrag.commons.notification.dto.MessageRecipient
import ai.sovereignrag.commons.notification.dto.MessageTemplateDto
import ai.sovereignrag.commons.notification.enumeration.DeliveryStatus
import ai.sovereignrag.commons.notification.enumeration.MessageChannel
import ai.sovereignrag.commons.notification.enumeration.MessagePriority
import ai.sovereignrag.commons.notification.enumeration.TemplateName
import ai.sovereignrag.commons.notification.enumeration.TemplateRecipientType
import ai.sovereignrag.commons.user.UserGateway
import ai.sovereignrag.notification.NotificationIntegration
import ai.sovereignrag.notification.domain.MessageTemplate
import ai.sovereignrag.notification.infrastructure.MessageEntity
import ai.sovereignrag.notification.infrastructure.MessageRepository
import ai.sovereignrag.notification.infrastructure.MessageTemplateRepository
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Locale

@Service
class MessagingService (
    private val messageRepository: MessageRepository,
    private val integrationConfigGateway: IntegrationConfigGateway,
    private val messageTemplateRepository: MessageTemplateRepository,
    private val applicationContext: ApplicationContext,
    private val userGateway: UserGateway
) {

    fun saveMessage(
        payload: MessagePayload, integratorId: String, templateName: TemplateName, content: String,
        recipient: MessageRecipient
    ): MessageDto {

        messageTemplateRepository.findByNameAndLocaleAndChannelAndRecipientTypeAndActiveIsTrue(
            templateName,
            payload.template.locale,
            payload.channel,
            payload.template.recipientType
        )?.let {

            return messageRepository.save(
                MessageEntity(
                    payload.channel,
                    it,
                    recipient.address,
                    DeliveryStatus.PENDING,
                    content,
                    null,
                    integratorId,
                    payload.priority,
                    payload.clientIdentifier,
                    payload.template.locale,
                    status = "PENDING"
                )
            ).toDomain().toDto()
        }

        throw RecordNotFoundException("Unable to find Message Template with name: $templateName")
    }

    fun findTemplateByNameAndLocaleAndChannelAndRecipientType(
        templateName: TemplateName, locale: Locale, messageChannel: MessageChannel,
        recipientType: TemplateRecipientType
    ): MessageTemplate? {

        return messageTemplateRepository.findByNameAndLocaleAndChannelAndRecipientTypeAndActiveIsTrue(
            templateName, locale, messageChannel, recipientType
        )?.toDomain()
    }

    fun updateMessageResponse(messageId: String, response: String?, status: DeliveryStatus) {
        messageRepository.updateMessageResponse(messageId, response, status)
    }

    fun generateTextFromTemplate(parameters: Map<String, String>, template: MessageTemplate): String {
        TODO("Not yet implemented")
    }

    @Transactional
    fun sendMessage(
        recipient: MessageRecipient, template: MessageTemplateDto, channel: MessageChannel,
        messagePriority: MessagePriority, clientIdentifier: String, parameters: Map<String, Any>,
        integrationAction: IntegrationConfigAction, content: String
    ): SendMessageResult {

        val payload = MessagePayload(recipient, template, channel, messagePriority, parameters, clientIdentifier, content)

        val integrationId =
            integrationConfigGateway.getIntegrationConfigId(
                integrationAction,
                userGateway.getSystemUserId().toString()
            )
                ?: throw RecordNotFoundException("No integration found for action: $integrationAction ")

        val integration = applicationContext.getBean(
            integrationId,
            NotificationIntegration::class
        ) as NotificationIntegration

        val message = saveMessage(
            payload,
            integration.getId(),
            template.name,
            ObjectMapperFacade.writeValueAsString(payload.parameters),
            recipient,
        )

        val result = integration.sendMessage(payload, template)

        updateMessageResponse(message.clientIdentifier, result.response, result.status)

        return SendMessageResult(result.status, message.clientIdentifier)
    }

    @Transactional
    fun sendMessageWithCc(
        recipients: List<MessageRecipient>, template: MessageTemplateDto, channel: MessageChannel,
        messagePriority: MessagePriority, clientIdentifier: String, parameters: Map<String, Any>,
        integrationAction: IntegrationConfigAction, content: String
    ): SendMessageResult {

        val integrationId =
            integrationConfigGateway.getIntegrationConfigId(
                integrationAction,
                userGateway.getSystemUserId().toString()
            )
                ?: throw RecordNotFoundException("No integration found for action: $integrationAction ")

        val integration = applicationContext.getBean(
            integrationId,
            NotificationIntegration::class
        ) as NotificationIntegration

        val groupRecipient = MessageRecipient(
            recipients.joinToString(", ") { it.address },
            "Group Email"
        )
        val payload = MessagePayload(groupRecipient, template, channel, messagePriority, parameters, clientIdentifier, content)
        val message = saveMessage(
            payload,
            integration.getId(),
            template.name,
            ObjectMapperFacade.writeValueAsString(payload.parameters),
            groupRecipient
        )

        val result = if (integration.getId() == "SEND_GRID_EMAIL") {

            try {
                val sendMessageMethod = integration.javaClass.getMethod(
                    "sendMessage",
                    List::class.java,
                    MessageTemplateDto::class.java,
                    Map::class.java,
                    Boolean::class.java
                )
                sendMessageMethod.invoke(integration, recipients, template, parameters, true) as ai.sovereignrag.commons.notification.dto.MessageSentResult
            } catch (e: Exception) {

                var lastResult: ai.sovereignrag.commons.notification.dto.MessageSentResult? = null
                recipients.forEach { recipient ->
                    val individualPayload = MessagePayload(recipient, template, channel, messagePriority, parameters, clientIdentifier, content)
                    lastResult = integration.sendMessage(individualPayload, template)
                }
                lastResult ?: ai.sovereignrag.commons.notification.dto.MessageSentResult(
                    DeliveryStatus.FAILED,
                    "No recipients to send to"
                )
            }
        } else {
            var lastResult: ai.sovereignrag.commons.notification.dto.MessageSentResult? = null
            recipients.forEach { recipient ->
                val individualPayload = MessagePayload(recipient, template, channel, messagePriority, parameters, clientIdentifier, content)
                lastResult = integration.sendMessage(individualPayload, template)
            }
            lastResult ?: ai.sovereignrag.commons.notification.dto.MessageSentResult(
                DeliveryStatus.FAILED,
                "No recipients to send to"
            )
        }

        updateMessageResponse(message.clientIdentifier, result.response, result.status)

        return SendMessageResult(result.status, message.clientIdentifier)
    }

}