package ai.sovereignrag.commons.accounting

import ai.sovereignrag.commons.accounting.dto.CreateTransactionPayload
import ai.sovereignrag.commons.accounting.dto.CreateTransactionResult
import ai.sovereignrag.commons.accounting.dto.RecentTransactionDto
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.util.UUID

interface TransactionGateway {
    
    fun createTransaction(payload: CreateTransactionPayload, senderAccount: AccountDto, recipientAccount: AccountDto): CreateTransactionResult

    fun reverseTransaction(transactionReference: UUID)

    fun void(transactionReference: UUID)

    fun findByInternalReference(internalReference: UUID): TransactionDto?

    fun getProcessIdByInternalReference(internalReference: UUID): UUID?

    fun findByExternalReference(externalReference: String): TransactionDto?

    fun findByDisplayReference(displayReference: String): TransactionDto?

    fun findByExternalReferenceAndType(externalReference: String, type: TransactionType): TransactionDto?

    fun findByCustomerIdAndMerchantId(customerId: String, merchantId: UUID, pageable: Pageable): Page<TransactionDto>

    fun completePendingTransaction(externalReference: String)

    fun completePendingTransaction(internalReference: UUID)
    
    fun searchTransactions(searchTerm: String, pageable: Pageable): Page<RecentTransactionDto>

    fun searchTransactions(searchTerm: String, accountId: UUID?, pageable: Pageable): Page<RecentTransactionDto>

    fun findAllByExternalReferences(references: Set<String>) : List<TransactionDto>

    fun findTransactionTypesByDisplayReferences(references: Set<String>) : List<TransactionReferenceDto>

    fun addProperty(internalReference: UUID, name: TransactionPropertyName, value: String)
}