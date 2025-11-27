package ai.sovereignrag.commons.notification

import ai.sovereignrag.commons.notification.dto.MessageRecipient
import ai.sovereignrag.commons.notification.enumeration.MessageChannel
import ai.sovereignrag.commons.notification.enumeration.MessagePriority
import ai.sovereignrag.commons.notification.enumeration.RecipientType
import ai.sovereignrag.commons.notification.enumeration.TemplateName
import java.util.Locale

interface NotificationGateway {

    fun sendNotification(
        recipient: MessageRecipient, templateName: TemplateName,
        channel: MessageChannel, priority: MessagePriority,
        parameters: Map<String, Any>, locale: Locale, clientIdentifier: String, recipientType: RecipientType
    ): SendMessageResult

    fun sendNotificationWithCc(
        recipients: List<MessageRecipient>, templateName: TemplateName,
        channel: MessageChannel, priority: MessagePriority,
        parameters: Map<String, Any>, locale: Locale, clientIdentifier: String, recipientType: RecipientType
    ): SendMessageResult
}