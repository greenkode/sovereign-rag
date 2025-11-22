package ai.sovereignrag.commons.messaging.dto

import ai.sovereignrag.commons.messaging.enumeration.MessageChannel

data class MessageTemplateDto(
    val channel: MessageChannel,
    val content: String,
    val title: String,
    val externalId: String,
    val active: Boolean
)
