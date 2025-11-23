package ai.sovereignrag.commons.notification.dto

import ai.sovereignrag.commons.notification.enumeration.NotificationType
import java.time.Instant
import java.util.UUID

data class WebhookConfigurationDto(
    val publicId: UUID,
    val merchantId: UUID,
    val webhookUrl: String,
    val notificationType: NotificationType,
    val isActive: Boolean,
    val secretKey: String?,
    val customHeaders: Map<String, String>?,
    val retryAttempts: Int,
    val timeoutSeconds: Int,
    val description: String?,
    val createdDate: Instant?,
    val lastModifiedDate: Instant?
)

data class CreateWebhookConfigurationRequest(
    val webhookUrl: String,
    val notificationType: NotificationType,
    val secretKey: String? = null,
    val customHeaders: Map<String, String>? = null,
    val description: String? = null
)

data class UpdateWebhookConfigurationRequest(
    val webhookUrl: String? = null,
    val isActive: Boolean? = null,
    val secretKey: String? = null,
    val customHeaders: Map<String, String>? = null,
    val description: String? = null
)

data class WebhookDeliveryRequest(
    val webhookUrl: String,
    val payload: Map<String, Any>,
    val headers: Map<String, String>,
    val retryAttempts: Int,
    val timeoutSeconds: Int,
    val secretKey: String?
)