package ai.sovereignrag.commons.notification.dto

import ai.sovereignrag.commons.notification.enumeration.NotificationChannel
import ai.sovereignrag.commons.notification.enumeration.NotificationType
import java.util.UUID


data class NotificationDeviceDto(
    val publicId: UUID,
    val notificationChannel: NotificationChannel,
    val value: String,
    val userId: UUID,
    val notificationType: NotificationType,
)
