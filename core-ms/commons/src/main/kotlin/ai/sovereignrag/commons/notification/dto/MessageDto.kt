package ai.sovereignrag.commons.notification.dto

import ai.sovereignrag.commons.notification.enumeration.DeliveryStatus
import ai.sovereignrag.commons.notification.enumeration.MessageChannel
import ai.sovereignrag.commons.notification.enumeration.MessagePriority
import ai.sovereignrag.commons.notification.enumeration.TemplateName
import java.time.Instant
import java.util.Locale

data class MessageDto(val channel: MessageChannel,
                      val template: TemplateName,
                      val deliveryStatus: DeliveryStatus,
                      val integrator: String,
                      val priority: MessagePriority,
                      val clientIdentifier: String,
                      val locale: Locale,
                      val status: String?,
                      val deliveryDate: Instant?,)