package ai.sovereignrag.commons.notification.enumeration

enum class NotificationChannel(val routingKey: String) {
    SMS("sms"),
    EMAIL("email"),
    PUSH("firebase"),
    IN_APP("inapp"),
    WHATSAPP("whatsapp"),
    TELEGRAM("telegram"),
    SPEAKER("speaker"),
    WEBHOOK("webhook");
}
