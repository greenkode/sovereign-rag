package ai.sovereignrag.commons.notification.dto

import ai.sovereignrag.commons.notification.enumeration.MessageChannel
import ai.sovereignrag.commons.notification.enumeration.MessagePriority
import ai.sovereignrag.commons.notification.enumeration.RecipientType
import ai.sovereignrag.commons.notification.enumeration.TemplateName
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import java.util.Locale

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class SendNotificationRequest(
    val recipients: List<MessageRecipient>,
    val templateName: TemplateName,
    val channel: MessageChannel,
    val priority: MessagePriority,
    val parameters: Map<String, String>,
    val locale: Locale,
    val clientIdentifier: String,
    val recipientType: RecipientType
)
