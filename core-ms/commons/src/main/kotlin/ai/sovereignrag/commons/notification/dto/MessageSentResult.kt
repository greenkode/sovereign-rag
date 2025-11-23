package ai.sovereignrag.commons.notification.dto

import ai.sovereignrag.commons.notification.enumeration.DeliveryStatus

data class MessageSentResult(val status: DeliveryStatus, val response: String? = null, val sentCode: String? = null)