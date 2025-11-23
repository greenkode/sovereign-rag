package ai.sovereignrag.process.service

import ai.sovereignrag.commons.accounting.AccountAddressType
import ai.sovereignrag.commons.accounting.AccountGateway
import ai.sovereignrag.commons.accounting.AccountType
import ai.sovereignrag.commons.accounting.EntryType
import ai.sovereignrag.commons.accounting.TransactionGateway
import ai.sovereignrag.commons.accounting.TransactionStatus
import ai.sovereignrag.commons.accounting.TransactionType
import ai.sovereignrag.commons.accounting.dto.CreateTransactionPayload
import ai.sovereignrag.commons.accounting.dto.LedgerEntryDto
import ai.sovereignrag.commons.currency.DefaultCurrency.currency
import ai.sovereignrag.commons.exception.RecordNotFoundException
import ai.sovereignrag.commons.process.ProcessDto
import ai.sovereignrag.commons.process.enumeration.ProcessRequestDataName
import ai.sovereignrag.process.action.ReverseTransactionAction
import ai.sovereignrag.process.spi.ProcessService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.javamoney.moneta.Money
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class FundPoolAccountProcessService(
    private val accountGateway: AccountGateway,
    private val transactionGateway: TransactionGateway,
    private val processService: ProcessService,
    private val reverseTransactionAction: ReverseTransactionAction,
) {

    private val log = KotlinLogging.logger {}

    fun completeFundPoolAccount(process: ProcessDto) {

        log.info { "Completing fund pool account for process: ${process.publicId}" }

        process.externalReference?.let { transactionGateway.findByExternalReference(it) }?.let {
            log.info { "Transaction already exists for process: ${process.publicId}" }
            processService.completeProcess(process.publicId, process.getInitialRequest().id)
            return
        }

        transactionGateway.findByInternalReference(process.publicId)?.let {
            processService.completeProcess(process.publicId, process.getInitialRequest().id)
            return
        }

        val request = process.getInitialRequest()

        val amount = Money.parse(request.getDataValue(ProcessRequestDataName.AMOUNT))
        val externalReference = request.getDataValueOrNull(ProcessRequestDataName.EXTERNAL_REFERENCE)
        val narration = request.getDataValueOrNull(ProcessRequestDataName.NARRATION)

        val cashAccount =
            accountGateway.findByPublicId(UUID.fromString(request.getDataValue(ProcessRequestDataName.SENDER_ACCOUNT_ID)))
                ?: throw RecordNotFoundException("System cash account not found for currency: ${currency.currencyCode}")

        val poolAccount =
            accountGateway.findByPublicId(UUID.fromString(request.getDataValue(ProcessRequestDataName.RECIPIENT_ACCOUNT_ID)))
                ?: throw RecordNotFoundException("Recipient account not found")


        require(poolAccount.type == AccountType.POOL) { "Source account must be a POOL account" }
        require(cashAccount.type == AccountType.CASH) { "Target account must be a CASH account" }

        val equityAccount = accountGateway.findSystemEquityAccountByCurrency(currency) ?: throw RecordNotFoundException(
            "Equity account not found for currency: ${currency.currencyCode}"
        )

        log.info { "Creating fund pool account transaction: debiting pool account ${poolAccount.publicId}, crediting cash account ${cashAccount.publicId}" }


        val ledgerEntries = listOf(
            LedgerEntryDto(
                type = EntryType.AMOUNT,
                amount = amount,
                debitAccount = cashAccount,
                creditAccount = equityAccount,
                currencyUnit = currency,
                narration = narration ?: "Fund Pool Account - ${cashAccount.name} Amount: $amount",
                details = "Fund Pool Account - ${cashAccount.name} Amount: $amount",
            ),
            LedgerEntryDto(
                type = EntryType.AMOUNT,
                amount = amount,
                debitAccount = poolAccount,
                creditAccount = cashAccount,
                currencyUnit = currency,
                narration = narration ?: "Fund Pool Account - ${poolAccount.name} Amount: $amount",
                details = "Fund Pool Account - ${poolAccount.name} Amount: $amount",
            )
        )

        transactionGateway.createTransaction(
            CreateTransactionPayload(
                amount = amount,
                reference = process.publicId,
                transactionType = TransactionType.FUND_POOL_ACCOUNT,
                transactionStatus = TransactionStatus.COMPLETED,
                processPublicId = process.publicId,
                senderAccountAddress = poolAccount.addresses.firstOrNull { it.type == AccountAddressType.CHART_OF_ACCOUNTS }?.address
                    ?: poolAccount.publicId.toString(),
                recipientAccountAddress = cashAccount.addresses.firstOrNull { it.type == AccountAddressType.CHART_OF_ACCOUNTS }?.address
                    ?: cashAccount.publicId.toString(),
                customerDetails = poolAccount.publicId.toString(),
                entries = ledgerEntries,
                externalReference = externalReference
            ),
            poolAccount,
            cashAccount
        )

        log.info { "Successfully created fund pool account transaction for process: ${process.publicId}" }

        processService.completeProcess(process.publicId, request.id)
    }

    fun handleFundPoolAccountFailure(process: ProcessDto) {
        log.info { "Handling fund pool account failure for process: ${process.publicId}" }

        // Check if transaction exists and reverse it if needed
        transactionGateway.findByInternalReference(process.publicId)?.let { transaction ->
            log.info { "Reversing transaction ${transaction.internalReference} for failed fund pool account process" }
            transactionGateway.reverseTransaction(transaction.internalReference)
        }

        processService.failProcess(process.publicId)
    }

    fun handleFundPoolAccountExpiry(process: ProcessDto) {

        log.info { "Handling fund pool account expiry for process: ${process.publicId}" }

        transactionGateway.findByInternalReference(process.publicId)?.let { transaction ->
            log.info { "Reversing transaction ${transaction.internalReference} for expired fund pool account process" }
            transactionGateway.reverseTransaction(transaction.internalReference)
        }

        processService.expireProcess(process.publicId, false)
    }


    fun reversePendingTransaction(process: ProcessDto) {

        log.info { "Reversing pending transaction for process: ${process.publicId}" }

        reverseTransactionAction.execute(process)
    }
}