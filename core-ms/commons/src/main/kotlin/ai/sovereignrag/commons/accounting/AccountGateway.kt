package ai.sovereignrag.commons.accounting

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID
import javax.money.CurrencyUnit
import javax.money.MonetaryAmount

interface AccountGateway {

    fun findByUserIdAndCurrency(userId: UUID, currency: CurrencyUnit): AccountDto?

    fun findByPublicId(publicId: UUID): AccountDto?

    fun findDefaultAccountByUserIdAndAccountType(userId: UUID, accountType: AccountType): AccountDto?

    fun findByAddress(address: String): AccountDto?

    fun findById(id: Long): AccountDto?

    fun createAccount(payload: CreateAccountPayload): AccountDto

    fun postNoDebit(publicId: UUID)

    fun getAccountBalance(publicId: UUID): MonetaryAmount

    fun addAddress(accountId: UUID, dto: AccountAddressDto)

    fun findAllByUserId(userId: UUID) : List<AccountDto>

    fun findAllByPublicIds(publicIds: Set<UUID>) : List<AccountDto>

    fun findAllByUserId(userId: UUID, pageable: Pageable) : Page<AccountDto>

    fun findByUserIdAndPublicId(userId: UUID, publicId: UUID): AccountDto?

    fun blockAccount(publicId: UUID)

    fun unblockAccount(publicId: UUID)

    fun getSubAccountOfType(publicId: UUID, accountType: AccountType): AccountDto?
    
    fun getSubAccountsOfTypes(publicId: UUID, accountTypes: Set<AccountType>): Map<AccountType, AccountDto>

    fun findDefaultAccountByUserId(userId: UUID): AccountDto?

    fun findSystemCashAccountByCurrency(currency: CurrencyUnit): AccountDto?

    fun findSystemEquityAccountByCurrency(currency: CurrencyUnit): AccountDto?

    fun getAccountBalances(publicIds: Set<UUID>): Map<UUID, MonetaryAmount>

    fun findAllByType(type: AccountType, pageable: Pageable): Page<AccountDto>
}