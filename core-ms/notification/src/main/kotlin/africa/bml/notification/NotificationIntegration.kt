package ai.sovereignrag.notification

import ai.sovereignrag.commons.notification.dto.MessagePayload
import ai.sovereignrag.commons.notification.dto.MessageSentResult
import ai.sovereignrag.commons.notification.dto.MessageTemplateDto

interface NotificationIntegration {

    fun getId(): String

    fun sendMessage(payload: MessagePayload, template: MessageTemplateDto): MessageSentResult
}