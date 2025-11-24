package ai.sovereignrag.commons.billpay.dto

import java.util.UUID

data class CreateIntegrationLogPayload(
    val request: String,
    val response: String?,
    val error: String?,
    val externalReference: String,
    val internalReference: UUID,
    val inbound: Boolean
)