package ai.sovereignrag.commons.messaging.dto

import nl.compilot.ai.commons.messaging.enumeration.MessageChannel
import nl.compilot.ai.commons.messaging.enumeration.MessagePriority

data class MessagePayload(
    val recipient: MessageRecipient,
    val template: MessageTemplateDto,
    val channel: MessageChannel,
    val priority: MessagePriority,
    val parameters: Map<String, Any>,
    val content: String? = null
)
