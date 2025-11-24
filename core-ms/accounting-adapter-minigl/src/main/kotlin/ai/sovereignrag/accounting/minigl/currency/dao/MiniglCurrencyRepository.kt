package ai.sovereignrag.accounting.minigl.currency.dao

import ai.sovereignrag.accounting.GLSession
import ai.sovereignrag.accounting.entity.CurrencyEntity as Currency
import ai.sovereignrag.accounting.repository.CurrencyRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository

@Repository
class MiniglCurrencyRepository {

    @Autowired
    private lateinit var glSession: GLSession
    
    @Autowired
    private lateinit var currencyRepository: CurrencyRepository

    fun getCurrenciesByCodes(codes: Set<String>): List<Currency> {
        return currencyRepository.findAllByNameIn(codes).toList()
    }

    fun getCurrencyByCode(code: String): Currency? {
        val optional = currencyRepository.findByName(code)
        return if (optional.isPresent) optional.get() else null
    }
    
    fun getAllCurrencies(): List<Currency> {
        return currencyRepository.findAll().sortedBy { it.name }
    }
}