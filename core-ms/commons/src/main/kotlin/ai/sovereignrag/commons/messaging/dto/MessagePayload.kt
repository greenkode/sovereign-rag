package ai.sovereignrag.commons.messaging.dto

import ai.sovereignrag.commons.messaging.enumeration.MessageChannel
import ai.sovereignrag.commons.messaging.enumeration.MessagePriority

data class MessagePayload(
    val recipient: MessageRecipient,
    val template: MessageTemplateDto,
    val channel: MessageChannel,
    val priority: MessagePriority,
    val parameters: Map<String, Any>,
    val content: String? = null
)
