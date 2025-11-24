package ai.sovereignrag.notification.domain

import ai.sovereignrag.commons.notification.dto.MessageDto
import ai.sovereignrag.commons.notification.enumeration.DeliveryStatus
import ai.sovereignrag.commons.notification.enumeration.MessageChannel
import ai.sovereignrag.commons.notification.enumeration.MessagePriority
import ai.sovereignrag.commons.notification.enumeration.TemplateName
import java.time.Instant
import java.util.Locale

data class Message(
    val channel: MessageChannel,
    val template: TemplateName,
    val recipient: String?,
    val deliveryStatus: DeliveryStatus,
    val request: String,
    val response: String?,
    val integrator: String,
    val priority: MessagePriority,
    val clientIdentifier: String,
    val locale: Locale,
    val status: String?,
    val deliveryDate: Instant?,
    val id: Long?
) {
    fun toDto(): MessageDto {
        return MessageDto(
            channel,
            template,
            deliveryStatus,
            integrator,
            priority,
            clientIdentifier,
            locale,
            status,
            deliveryDate
        )
    }
}