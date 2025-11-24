package ai.sovereignrag.accounting.config

import ai.sovereignrag.commons.currency.CustomCurrencyProvider
import java.util.ServiceLoader
import javax.money.spi.CurrencyProviderSpi
import javax.money.spi.ServiceProvider

class DelegatingCurrencyServiceProvider : ServiceProvider {

    private val akupayProvider = CustomCurrencyProvider()

    override fun getPriority(): Int = 1

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getServices(serviceType: Class<T>): List<T> {
        return when (serviceType) {
            CurrencyProviderSpi::class.java -> listOf(
                akupayProvider as T,
            )
            else -> ServiceLoader.load(serviceType).toList()
        }
    }

    override fun <T : Any> getService(serviceType: Class<T>): T? =
        getServices(serviceType).firstOrNull()
}
