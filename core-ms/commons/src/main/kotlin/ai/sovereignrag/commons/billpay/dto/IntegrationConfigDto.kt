package ai.sovereignrag.commons.billpay.dto

import java.util.UUID

data class IntegrationConfigDto(val exchangeId: String, val poolAccountId: UUID)