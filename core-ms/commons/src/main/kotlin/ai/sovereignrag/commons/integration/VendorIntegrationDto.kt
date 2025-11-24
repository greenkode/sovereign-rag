package ai.sovereignrag.commons.integration

import java.util.UUID

data class VendorIntegrationDto(
    val publicId: UUID,
    val integrationId: String,
    val vendorName: String,
    val poolAccountId: UUID,
    val currencyCode: String
)