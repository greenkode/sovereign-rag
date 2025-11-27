package ai.sovereignrag.commons.notification.dto

import ai.sovereignrag.commons.notification.enumeration.MessageChannel
import ai.sovereignrag.commons.notification.enumeration.RecipientType
import ai.sovereignrag.commons.notification.enumeration.TemplateName
import java.util.Locale

data class MessageTemplateDto(val channel: MessageChannel,
                              val content: String,
                              val title: String,
                              val name: TemplateName,
                              val locale: Locale,
                              val externalId: String,
                              val active: Boolean,
                              val recipientType: RecipientType
)