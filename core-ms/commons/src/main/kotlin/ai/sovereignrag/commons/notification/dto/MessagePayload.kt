package ai.sovereignrag.commons.notification.dto

import ai.sovereignrag.commons.notification.enumeration.MessageChannel
import ai.sovereignrag.commons.notification.enumeration.MessagePriority

data class MessagePayload(
    val recipient: MessageRecipient, val template: MessageTemplateDto,
    val channel: MessageChannel, val priority: MessagePriority,
    val parameters: Map<String, Any>, val clientIdentifier: String,
    val content: String? = null
)