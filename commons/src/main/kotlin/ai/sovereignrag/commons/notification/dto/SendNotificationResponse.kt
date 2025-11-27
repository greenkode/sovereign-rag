package ai.sovereignrag.commons.notification.dto

import ai.sovereignrag.commons.notification.enumeration.DeliveryStatus
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class SendNotificationResponse(
    val status: DeliveryStatus,
    val messageId: String,
    val message: String? = null
)
