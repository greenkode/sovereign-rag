package ai.sovereignrag.process.action.lien

import ai.sovereignrag.accounting.transaction.spi.TransactionService
import ai.sovereignrag.commons.accounting.AccountGateway
import ai.sovereignrag.commons.accounting.AccountType
import ai.sovereignrag.commons.accounting.EntryType
import ai.sovereignrag.commons.accounting.TransactionStatus
import ai.sovereignrag.commons.accounting.TransactionType
import ai.sovereignrag.commons.accounting.dto.CreateTransactionPayload
import ai.sovereignrag.commons.accounting.dto.LedgerEntryDto
import ai.sovereignrag.commons.exception.BmlServiceException
import ai.sovereignrag.commons.exception.RecordNotFoundException
import ai.sovereignrag.commons.exception.TransactionProcessingException
import ai.sovereignrag.commons.process.ProcessDto
import ai.sovereignrag.commons.process.enumeration.ProcessRequestDataName
import ai.sovereignrag.commons.process.enumeration.ProcessRequestType
import ai.sovereignrag.commons.process.enumeration.ProcessState
import io.github.oshai.kotlinlogging.KotlinLogging.logger
import org.javamoney.moneta.Money
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class UnlienAmountAction(
    private val transactionService: TransactionService,
    private val accountGateway: AccountGateway,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {

    private val log = logger {}

    fun execute(process: ProcessDto) {

        val request = process.requests.first { r -> r.type == ProcessRequestType.CREATE_NEW_PROCESS }

        val unblockRequest =
            process.requests.first { r -> r.type == ProcessRequestType.UNLIEN_AMOUNT && r.state == ProcessState.PENDING }

        try {
            val originalAmount = Money.parse(request.getDataValue(ProcessRequestDataName.AMOUNT))

            val unblockAmount = Money.parse(unblockRequest.getDataValue(ProcessRequestDataName.AMOUNT))

            val totalUnblockedAmount =
                process.requests.filter { it.type == ProcessRequestType.UNLIEN_AMOUNT && it.state == ProcessState.COMPLETE }
                    .map {
                        Money.parse(it.getDataValue(ProcessRequestDataName.AMOUNT))
                    }.takeIf { it.isNotEmpty() }?.reduce { acc, money -> acc.add(money) }
                    ?: Money.zero(originalAmount.currency)

            if (totalUnblockedAmount.isGreaterThanOrEqualTo(originalAmount) || unblockAmount.isGreaterThan(
                    originalAmount.subtract(totalUnblockedAmount)
                )
            )
                throw TransactionProcessingException("Amount to unlock is greater than the original amount")

            val account = accountGateway.findByPublicId(
                UUID.fromString(request.getDataValue(ProcessRequestDataName.ACCOUNT_PUBLIC_ID))
            ) ?: throw RecordNotFoundException("Account details not found")

            val address = request.getDataValue(ProcessRequestDataName.ACCOUNT_ADDRESS)

            val lockAccount =
                accountGateway.getSubAccountOfType(account.publicId, AccountType.LOCK)
                    ?: throw RecordNotFoundException(
                        "Account details not found"
                    )

            transactionService.createTransaction(
                CreateTransactionPayload(
                    unblockAmount,
                    UUID.randomUUID(),
                    TransactionType.UNLIEN_AMOUNT,
                    TransactionStatus.COMPLETED,
                    process.publicId,
                    address,
                    address,
                    address,
                    listOf(
                        LedgerEntryDto(
                            EntryType.AMOUNT,
                            unblockAmount,
                            lockAccount,
                            account,
                            account.currency,
                            request.getDataValue(ProcessRequestDataName.NARRATION),
                            TransactionType.LIEN_AMOUNT.description
                        )
                    )
                ),
                lockAccount, account
            )
            applicationEventPublisher.publishEvent(
                UnlienAmountEvent(
                    unblockRequest.id,
                    ProcessState.COMPLETE,
                    process.publicId
                )
            )
        } catch (e: Exception) {

            applicationEventPublisher.publishEvent(
                UnlienAmountEvent(
                    unblockRequest.id,
                    ProcessState.FAILED,
                    process.publicId,
                    e.message!!
                )
            )

            log.error(e) { "Error occurred while unblocking amount: ${e.message}" }

            if (e is BmlServiceException) throw e

            throw TransactionProcessingException(e.message!!)
        }
    }
}