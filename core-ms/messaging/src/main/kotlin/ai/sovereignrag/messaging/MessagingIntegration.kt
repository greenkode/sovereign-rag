package ai.sovereignrag.messaging

import ai.sovereignrag.commons.messaging.dto.MessagePayload
import ai.sovereignrag.commons.messaging.dto.MessageSentResult
import ai.sovereignrag.commons.messaging.dto.MessageTemplateDto

interface MessagingIntegration {

    fun getId(): String

    fun sendMessage(payload: MessagePayload, template: MessageTemplateDto): MessageSentResult
}