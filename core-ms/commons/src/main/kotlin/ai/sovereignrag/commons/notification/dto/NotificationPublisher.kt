package ai.sovereignrag.commons.notification.dto

interface NotificationPublisher {
    fun publish(obj: NotificationEventPayload)

}