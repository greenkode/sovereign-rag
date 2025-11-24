package ai.sovereignrag.accounting.minigl.gateway

import ai.sovereignrag.accounting.minigl.account.service.MiniglAccountService
import ai.sovereignrag.commons.accounting.AccountBalanceDto
import ai.sovereignrag.commons.accounting.CreatedAccountDetails
import ai.sovereignrag.commons.accounting.AccountType
import ai.sovereignrag.commons.accounting.MiniglAccountGateway
import ai.sovereignrag.commons.currency.DefaultCurrency
import ai.sovereignrag.commons.exception.AccountServiceException
import ai.sovereignrag.commons.performance.LogExecutionTime
import org.springframework.stereotype.Service
import java.util.UUID
import javax.money.CurrencyUnit
import javax.money.Monetary
import javax.money.MonetaryAmount

@Service
class MiniglAccountGatewayAdapter(
    private val miniglAccountService: MiniglAccountService
) : MiniglAccountGateway {

    @LogExecutionTime
    override fun getAccountBalance(code: String): AccountBalanceDto {
        val chart = miniglAccountService.getDefaultChart()
            ?: throw AccountServiceException("Default chart not found")
        
        val account = miniglAccountService.getAccountByChartAndCode(chart, code)
            ?: throw AccountServiceException("Unable to get account for code: $code")
        
        val currency = Monetary.getCurrency(account.currencyCode ?: DefaultCurrency.currency.currencyCode)
        val balance = miniglAccountService.getAccountBalance(account, currency)
        
        return AccountBalanceDto(account.id, balance)
    }

    @LogExecutionTime
    override fun createAccount(
        currency: CurrencyUnit,
        publicId: UUID,
        type: AccountType,
        metadata: Map<String, String>
    ): CreatedAccountDetails {
        val chart = miniglAccountService.getDefaultChart()
            ?: throw AccountServiceException("Default chart not found")
        
        val parentAccount = miniglAccountService.getCompositeAccountByChartAndDescription(chart, type.defaultParent)
            ?: throw AccountServiceException("Parent account not found: ${type.defaultParent}")
        
        val account = miniglAccountService.createAccount(
            chart,
            parentAccount.code,
            currency.currencyCode,
            publicId.toString(),
            metadata,
            type.padding,
            true,
            type.name
        )
        
        val balance = miniglAccountService.getAccountBalance(account, currency)
        
        return CreatedAccountDetails(
            account.id,
            account.code,
            currency,
            balance
        )
    }

    @LogExecutionTime
    override fun getAccountBalances(ids: Set<String>): Map<UUID, MonetaryAmount> {
        val chart = miniglAccountService.getDefaultChart()
            ?: throw AccountServiceException("Default chart not found")

        val balances = miniglAccountService.getAccountBalances(chart, ids)

        val accounts = miniglAccountService.findAllByCodes(ids)
        val accountsByCode = accounts.associateBy { it.code }

        return balances.mapNotNull { (code, monetaryAmount) ->
            val account = accountsByCode[code]
            account?.let { UUID.fromString(it.description) to monetaryAmount }
        }.toMap()
    }

    @LogExecutionTime
    override fun getRunningBalanceByCode(code: String, transactionReference: UUID): MonetaryAmount {
        val chart = miniglAccountService.getDefaultChart()
            ?: throw AccountServiceException("Default chart not found")

        val account = miniglAccountService.getAccountByChartAndCode(chart, code)
            ?: throw AccountServiceException("Unable to get account for code: $code")

        val currency = Monetary.getCurrency(account.currencyCode ?: DefaultCurrency.currency.currencyCode)

        return miniglAccountService.getAccountRunningBalance(account, currency, transactionReference.toString())
    }
}