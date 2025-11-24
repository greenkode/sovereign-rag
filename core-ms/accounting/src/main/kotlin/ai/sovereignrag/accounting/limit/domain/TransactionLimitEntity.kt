package ai.sovereignrag.accounting.limit.domain

import ai.sovereignrag.commons.accounting.AccountType
import ai.sovereignrag.commons.accounting.TransactionType
import ai.sovereignrag.commons.model.AuditableEntity
import ai.sovereignrag.commons.currency.CurrencyUnitConverter
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.javamoney.moneta.Money
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import javax.money.CurrencyUnit
import javax.money.MonetaryAmount

@Entity
@Table(name = "transaction_limit")
class TransactionLimitEntity(

    val profileId: UUID,

    @Enumerated(EnumType.STRING)
    val transactionType: TransactionType,

    @Enumerated(EnumType.STRING)
    val accountType: AccountType,

    @Convert(converter = CurrencyUnitConverter::class)
    val currency: CurrencyUnit,

    maxDailyDebit: MonetaryAmount,

    maxDailyCredit: MonetaryAmount,

    cumulativeDebit: MonetaryAmount,

    cumulativeCredit: MonetaryAmount,

    minTransactionDebit: MonetaryAmount,

    minTransactionCredit: MonetaryAmount,

    maxTransactionDebit: MonetaryAmount,

    maxTransactionCredit: MonetaryAmount,

    maxAccountBalance: MonetaryAmount,

    val start: Instant,

    val expiry: Instant?,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null
) : AuditableEntity() {

    private val maxDailyDebit: BigDecimal = maxDailyDebit.number.numberValue(BigDecimal::class.java)

    private val maxDailyCredit: BigDecimal = maxDailyDebit.number.numberValue(BigDecimal::class.java)

    private val cumulativeCredit: BigDecimal = maxDailyDebit.number.numberValue(BigDecimal::class.java)

    private val cumulativeDebit: BigDecimal = maxDailyDebit.number.numberValue(BigDecimal::class.java)

    private val minTransactionDebit: BigDecimal = maxDailyDebit.number.numberValue(BigDecimal::class.java)

    private val minTransactionCredit: BigDecimal = maxDailyDebit.number.numberValue(BigDecimal::class.java)

    private val maxTransactionDebit: BigDecimal = maxDailyDebit.number.numberValue(BigDecimal::class.java)

    private val maxTransactionCredit: BigDecimal = maxDailyDebit.number.numberValue(BigDecimal::class.java)

    private val maxAccountBalance: BigDecimal = maxDailyDebit.number.numberValue(BigDecimal::class.java)

    fun maxDailyDebit() = Money.of(maxDailyDebit, currency)

    fun maxDailyCredit() = Money.of(maxDailyCredit, currency)

    fun cumulativeCredit() = Money.of(cumulativeCredit, currency)

    fun cumulativeDebit() = Money.of(cumulativeDebit, currency)

    fun minTransactionDebit() = Money.of(minTransactionDebit, currency)

    fun minTransactionCredit() = Money.of(minTransactionCredit, currency)

    fun maxTransactionDebit() = Money.of(maxTransactionDebit, currency)

    fun maxTransactionCredit() = Money.of(maxTransactionCredit, currency)

    fun maxAccountBalance() = Money.of(maxAccountBalance, currency)

    fun toDomain() = TransactionLimit(
        id!!,
        profileId,
        transactionType,
        accountType,
        currency,
        maxDailyDebit(),
        maxDailyCredit(),
        cumulativeCredit(),
        cumulativeDebit(),
        minTransactionDebit(),
        minTransactionCredit(),
        maxDailyDebit(),
        maxTransactionCredit(),
        maxAccountBalance(),
        start,
        expiry
    )
}