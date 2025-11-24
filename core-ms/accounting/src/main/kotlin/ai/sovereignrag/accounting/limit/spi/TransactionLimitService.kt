package ai.sovereignrag.accounting.limit.spi

import ai.sovereignrag.accounting.limit.domain.TransactionLimit
import ai.sovereignrag.accounting.limit.domain.TransactionLimitRepository
import ai.sovereignrag.commons.accounting.TransactionLimitGateway
import ai.sovereignrag.commons.accounting.TransactionType
import ai.sovereignrag.commons.accounting.dto.TransactionLimitDto
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID
import javax.money.CurrencyUnit

@Service
class TransactionLimitService(private val transactionLimitRepository: TransactionLimitRepository) :
    TransactionLimitGateway {

    fun findByUserProfileAndTransactionType(
        profileId: UUID,
        transactionType: TransactionType,
        currency: CurrencyUnit
    ): TransactionLimit? {

        return transactionLimitRepository.findByProfileIdAndTransactionTypeAndCurrencyAndStartBeforeAndExpiryIsNullOrExpiryAfter(
            profileId, transactionType, currency,
            Instant.now(), Instant.now()
        )?.toDomain()
    }

    override fun findByProfileAndTransactionType(
        profileId: UUID,
        transactionType: TransactionType,
        currency: CurrencyUnit
    ): TransactionLimitDto? {

        return transactionLimitRepository.findByProfileIdAndTransactionTypeAndCurrencyAndStartBeforeAndExpiryIsNullOrExpiryAfter(
            profileId, transactionType, currency,
            Instant.now(), Instant.now()
        )?.toDomain()
            ?.let {
                TransactionLimitDto(
                    it.id,
                    it.profileId,
                    it.transactionType,
                    it.accountType,
                    it.currency,
                    it.maxDailyDebit,
                    it.maxDailyCredit,
                    it.cumulativeCredit,
                    it.cumulativeDebit,
                    it.minTransactionDebit,
                    it.minTransactionCredit,
                    it.maxTransactionDebit,
                    it.maxTransactionCredit,
                    it.maxAccountBalance,
                    it.start,
                    it.expiry
                )
            }
    }
}