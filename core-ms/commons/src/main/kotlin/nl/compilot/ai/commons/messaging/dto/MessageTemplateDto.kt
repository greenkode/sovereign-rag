package nl.compilot.ai.commons.messaging.dto

import nl.compilot.ai.commons.messaging.enumeration.MessageChannel

data class MessageTemplateDto(
    val channel: MessageChannel,
    val content: String,
    val title: String,
    val externalId: String,
    val active: Boolean
)
