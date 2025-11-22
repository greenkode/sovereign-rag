package ai.sovereignrag.commons.messaging.dto

import nl.compilot.ai.commons.messaging.enumeration.DeliveryStatus

data class MessageSentResult(
    val status: DeliveryStatus,
    val response: String? = null,
    val sentCode: String? = null
)
