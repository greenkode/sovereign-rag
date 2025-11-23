package ai.sovereignrag.commons.notification

import ai.sovereignrag.commons.notification.dto.CreateWebhookConfigurationRequest
import ai.sovereignrag.commons.notification.dto.UpdateWebhookConfigurationRequest
import ai.sovereignrag.commons.notification.dto.WebhookConfigurationDto
import ai.sovereignrag.commons.notification.enumeration.NotificationType
import java.util.UUID

interface WebhookNotificationGateway {

    fun createWebhookConfiguration(
        merchantId: UUID,
        request: CreateWebhookConfigurationRequest
    ): WebhookConfigurationDto
    
    fun updateWebhookConfiguration(
        publicId: UUID,
        request: UpdateWebhookConfigurationRequest
    ): WebhookConfigurationDto
    
    fun getWebhookConfiguration(publicId: UUID): WebhookConfigurationDto
    
    fun getMerchantWebhookConfigurations(merchantId: UUID): List<WebhookConfigurationDto>
    
    fun deleteWebhookConfiguration(publicId: UUID)
    
    fun deactivateWebhookConfiguration(publicId: UUID)
    
    fun reactivateWebhookConfiguration(publicId: UUID): WebhookConfigurationDto
    
    fun sendWebhookNotification(
        merchantId: UUID,
        notificationType: NotificationType,
        payload: Map<String, Any>,
        eventId: String = UUID.randomUUID().toString()
    )
}