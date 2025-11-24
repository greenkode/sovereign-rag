package ai.sovereignrag.commons.integration

import ai.sovereignrag.commons.enumeration.ResponseCode
import ai.sovereignrag.commons.enumeration.TransactionFollowUpAction
import java.util.UUID

data class IntegrationResponseCodeDto(
    val publicId: UUID,
    val integrationName: String,
    val integratorCode: String,
    val internalResponseCode: ResponseCode,
    val followUpAction: TransactionFollowUpAction,
    val isActive: Boolean
)