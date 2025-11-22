package nl.compilot.ai.messaging

import nl.compilot.ai.commons.messaging.dto.MessagePayload
import nl.compilot.ai.commons.messaging.dto.MessageSentResult
import nl.compilot.ai.commons.messaging.dto.MessageTemplateDto

interface MessagingIntegration {

    fun getId(): String

    fun sendMessage(payload: MessagePayload, template: MessageTemplateDto): MessageSentResult
}
