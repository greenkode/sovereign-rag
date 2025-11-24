package ai.sovereignrag.commons.integration

import java.util.UUID
import javax.money.CurrencyUnit


interface VendorIntegrationConfigGateway {
    
    fun getPoolAccount(integrationId: String, currency: CurrencyUnit): UUID?

    fun getPoolAccount(integrationId: String, currencyCode: String): UUID?

    fun getPoolAccountsForIntegration(integrationId: String): Map<String, UUID>

    fun getVendorByPoolAccount(poolAccountId: UUID, currencyCode: String): VendorIntegrationDto?

    fun getVendorByPublicId(publicId: UUID): VendorIntegrationDto?

    fun getVendorByIntegrationId(integrationId: String): VendorIntegrationDto?

    fun getAllVendorIntegrations(): List<VendorIntegrationDto>

    fun getAllPoolAccountsByIntegration(): Map<String, VendorPoolAccountInfo>

    fun getAllCurrencies(): Set<String>

    fun getAllPoolAccountIds(): Set<UUID>
}