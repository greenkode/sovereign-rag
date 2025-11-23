package ai.sovereignrag.notification.domain

import ai.sovereignrag.commons.notification.dto.MessageTemplateDto
import ai.sovereignrag.commons.notification.enumeration.MessageChannel
import ai.sovereignrag.commons.notification.enumeration.TemplateName
import ai.sovereignrag.commons.notification.enumeration.TemplateRecipientType
import java.util.Locale

data class MessageTemplate(
    val channel: MessageChannel,
    val content: String,
    val title: String,
    val name: TemplateName,
    val locale: Locale,
    val externalId: String,
    val active: Boolean,
    val recipientType: TemplateRecipientType,
    val id: Long
) {
    fun toDto(): MessageTemplateDto {
        return MessageTemplateDto(channel, content, title, name, locale, externalId, active, recipientType)
    }
}