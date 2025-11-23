package ai.sovereignrag.commons.notification

import ai.sovereignrag.commons.notification.enumeration.DeliveryStatus

data class SendMessageResult(val status: DeliveryStatus, val messageId: String)