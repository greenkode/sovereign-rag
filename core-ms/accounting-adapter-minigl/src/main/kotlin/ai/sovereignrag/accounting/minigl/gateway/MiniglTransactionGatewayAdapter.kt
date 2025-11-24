package ai.sovereignrag.accounting.minigl.gateway

import ai.sovereignrag.accounting.gateway.api.request.CreateTransactionRequest
import ai.sovereignrag.accounting.gateway.api.request.TransactionEntryRequest
import ai.sovereignrag.accounting.minigl.account.service.MiniglAccountService
import ai.sovereignrag.accounting.minigl.transaction.service.MiniglTransactionService
import ai.sovereignrag.commons.accounting.AccountAddressType
import ai.sovereignrag.commons.accounting.CreateTransactionDetailsDto
import ai.sovereignrag.commons.accounting.MiniglTransactionDto
import ai.sovereignrag.commons.accounting.MiniglTransactionGateway
import ai.sovereignrag.commons.accounting.TransactionLimitDto
import ai.sovereignrag.commons.accounting.TransactionType
import ai.sovereignrag.commons.accounting.dto.LedgerEntryDto
import ai.sovereignrag.commons.exception.AccountServiceException
import ai.sovereignrag.commons.monitoring.ProcessContext
import ai.sovereignrag.commons.performance.LogExecutionTime
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class MiniglTransactionGatewayAdapter(
    private val miniglTransactionService: MiniglTransactionService,
    private val accountService: MiniglAccountService
) : MiniglTransactionGateway {

    @LogExecutionTime
    @ProcessContext(processReferenceParam = "#transaction.processPublicId")
    @Transactional(transactionManager = "accountingTransactionManager")
    override fun createTransaction(
        transaction: MiniglTransactionDto,
        pending: Boolean,
        entries: ArrayList<LedgerEntryDto>,
        transactionLimit: TransactionLimitDto?,
    ): CreateTransactionDetailsDto {
        val chart = accountService.getDefaultChart()
            ?: throw AccountServiceException("Default chart not found")
        
        val transactionType = TransactionType.valueOf(transaction.type)
        
        val request = CreateTransactionRequest(
            transaction.reference,
            transactionType,
            transactionType.group, // Use the group from the transaction type
            pending,
            null, // Transaction limit mapping would be needed here
            mapOf(
                "display_ref" to transaction.displayRef,
                "customer_id" to transaction.customerDetails,
                "fee" to transaction.fee.number.toString(),
                "commission" to transaction.commission.number.toString(),
                "rebate" to transaction.rebate.number.toString(),
            ),
            entries.map { entry ->
                TransactionEntryRequest(
                    entry.narration,
                    entry.amount,
                    entry.debitAccount.addresses.first { it.type == AccountAddressType.CHART_OF_ACCOUNTS }.address,
                    entry.creditAccount.addresses.first { it.type == AccountAddressType.CHART_OF_ACCOUNTS }.address,
                    mapOf(
                        "type" to entry.type.name,
                        "details" to entry.details,
                        "skip_limits" to entry.debitAccount.type.skipLimitsCheck.toString(),
                        "sender_name" to entry.debitAccount.name,
                        "recipient_name" to entry.creditAccount.name
                    )
                )
            }
        )

        val result = miniglTransactionService.createTransaction(request, chart)
        
        return CreateTransactionDetailsDto(
            UUID.fromString(result.reference),
            result.metadata
        )
    }

    @LogExecutionTime
    @Transactional(transactionManager = "accountingTransactionManager")
    override fun completePendingTransaction(reference: UUID): CreateTransactionDetailsDto {
        val result = miniglTransactionService.completeTransaction(reference.toString())
        
        return CreateTransactionDetailsDto(
            UUID.fromString(result.reference),
            result.metadata
        )
    }

    @LogExecutionTime
    @Transactional(transactionManager = "accountingTransactionManager")
    override fun reverseTransaction(reference: UUID, reversalReference: UUID): CreateTransactionDetailsDto {
        val result = miniglTransactionService.reverseTransaction(reference.toString(), reversalReference.toString())

        return CreateTransactionDetailsDto(
            UUID.fromString(result.reference),
            result.metadata
        )
    }

}