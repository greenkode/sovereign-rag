package ai.sovereignrag.commons.integration

import java.util.UUID

data class VendorPoolAccountInfo(
    val integrationId: String,
    val vendorName: String,
    val currencyToPoolAccount: Map<String, UUID>
)