package ai.sovereignrag.process.action.lien

import ai.sovereignrag.accounting.transaction.spi.TransactionService
import ai.sovereignrag.commons.accounting.AccountGateway
import ai.sovereignrag.commons.accounting.AccountType
import ai.sovereignrag.commons.accounting.EntryType
import ai.sovereignrag.commons.accounting.TransactionStatus
import ai.sovereignrag.commons.accounting.TransactionType
import ai.sovereignrag.commons.accounting.dto.CreateTransactionPayload
import ai.sovereignrag.commons.accounting.dto.LedgerEntryDto
import ai.sovereignrag.commons.accounting.event.LienCreatedEvent
import ai.sovereignrag.commons.exception.RecordNotFoundException
import ai.sovereignrag.commons.process.ProcessDto
import ai.sovereignrag.commons.process.enumeration.ProcessRequestDataName
import io.github.oshai.kotlinlogging.KotlinLogging
import org.javamoney.moneta.Money
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

@Component
class LienAmountAction(
    private val accountGateway: AccountGateway,
    private val transactionService: TransactionService,
    private val events: ApplicationEventPublisher,
) {

    private val log = KotlinLogging.logger {}

    fun execute(process: ProcessDto) {


        log.info { "Creating lien for ${process.publicId}" }

        val request = process.getInitialRequest()

        val externalReference = request.getDataValue(ProcessRequestDataName.EXTERNAL_REFERENCE)

        val address = request.getDataValue(ProcessRequestDataName.ACCOUNT_ADDRESS)

        val account = accountGateway.findByAddress(address) ?: throw RecordNotFoundException(
            "Account not found with address: $address"
        )

        val lockAccount =
            accountGateway.getSubAccountOfType(account.publicId, AccountType.LOCK) ?: throw RecordNotFoundException(
                "Account not found with address: $address"
            )

        val amount = Money.parse(request.getDataValue(ProcessRequestDataName.AMOUNT))

        transactionService.createTransaction(
            CreateTransactionPayload(
                amount, process.publicId, TransactionType.LIEN_AMOUNT, TransactionStatus.COMPLETED,
                process.publicId, address, address,
                address
                ,listOf(
                    LedgerEntryDto(
                        EntryType.AMOUNT,
                        amount,
                        account,
                        lockAccount,
                        account.currency,
                        request.getDataValue(ProcessRequestDataName.NARRATION),
                        TransactionType.LIEN_AMOUNT.description
                    )
                ),
                externalReference = externalReference
            ), account, lockAccount
        )

        events.publishEvent(LienCreatedEvent(process.publicId, address))
    }
}