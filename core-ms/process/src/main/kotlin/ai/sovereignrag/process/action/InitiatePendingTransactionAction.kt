package ai.sovereignrag.process.action

import ai.sovereignrag.commons.accounting.AccountGateway
import ai.sovereignrag.commons.accounting.EntryType
import ai.sovereignrag.commons.accounting.TransactionGateway
import ai.sovereignrag.commons.accounting.TransactionStatus
import ai.sovereignrag.commons.accounting.TransactionType
import ai.sovereignrag.commons.accounting.dto.CreateTransactionPayload
import ai.sovereignrag.commons.accounting.dto.LedgerEntryDto
import ai.sovereignrag.commons.exception.RecordNotFoundException
import ai.sovereignrag.commons.exception.TransactionServiceException
import ai.sovereignrag.commons.process.ProcessDto
import ai.sovereignrag.commons.process.enumeration.ProcessRequestDataName
import jakarta.transaction.Transactional
import io.github.oshai.kotlinlogging.KotlinLogging
import org.javamoney.moneta.Money
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@Transactional
class InitiatePendingTransactionAction(
    private val accountGateway: AccountGateway,
    private val transactionGateway: TransactionGateway
) {

    private val log = KotlinLogging.logger {}

    fun execute(process: ProcessDto) {

        transactionGateway.findByInternalReference(process.publicId)?.let {
            return
        }

        val request = process.getInitialRequest()

        val amount = Money.parse(request.getDataValue(ProcessRequestDataName.AMOUNT))

        val transactionType = TransactionType.valueOf(request.getDataValue(ProcessRequestDataName.TRANSACTION_TYPE))

        val senderAccountId = UUID.fromString(request.data[ProcessRequestDataName.SENDER_ACCOUNT_ID])
        val recipientAccountId = UUID.fromString(request.getDataValue(ProcessRequestDataName.RECIPIENT_ACCOUNT_ID))
        
        val accounts = accountGateway.findAllByPublicIds(setOf(senderAccountId, recipientAccountId))
            .associateBy { it.publicId }
        
        val senderAccount = accounts[senderAccountId]
            ?: throw RecordNotFoundException("Unable to find account with id: $senderAccountId")
            
        val recipientAccount = accounts[recipientAccountId]
            ?: throw TransactionServiceException("Unable to find account with id: $recipientAccountId")

        log.info { "Sending fraud detection check request for ${process.publicId}" }

        val entries = listOf(
            LedgerEntryDto(
                EntryType.AMOUNT,
                amount,
                senderAccount,
                recipientAccount,
                senderAccount.currency,
                "BillPay: Vending $transactionType - $amount",
                "${senderAccount.name}: ${recipientAccount.name}",
            ),
        )

        transactionGateway.createTransaction(
            CreateTransactionPayload(
                amount,
                process.publicId,
                transactionType,
                TransactionStatus.PENDING,
                process.publicId,
                request.getDataValueOrEmpty(ProcessRequestDataName.SENDER_ACCOUNT_ADDRESS),
                request.getDataValue(ProcessRequestDataName.RECIPIENT_ACCOUNT_ADDRESS),
                request.getDataValue(ProcessRequestDataName.CUSTOMER_ID),
                entries,
                request.getDataValueOrNull(ProcessRequestDataName.INTEGRATOR_ID),
                request.getDataValueOrNull(ProcessRequestDataName.EXTERNAL_REFERENCE),
                request.getDataValueOrNull(ProcessRequestDataName.PRODUCT_ID)?.let { UUID.fromString(it) },
                process.getInitialRequest().getDataValueOrNull(ProcessRequestDataName.IS_LEND)?.toBoolean() == true
            ),
            senderAccount,
            recipientAccount
        )
    }
}
