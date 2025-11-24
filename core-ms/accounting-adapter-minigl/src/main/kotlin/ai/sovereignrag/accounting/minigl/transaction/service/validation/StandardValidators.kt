package ai.sovereignrag.accounting.minigl.transaction.service.validation

import ai.sovereignrag.accounting.gateway.api.request.CreateTransactionRequest
import ai.sovereignrag.accounting.minigl.common.MiniglConstants
import ai.sovereignrag.accounting.minigl.transaction.exception.TransactionLimitExceededException

class CurrencyConsistencyValidator : TransactionValidator() {
    override fun doValidate(request: CreateTransactionRequest, context: ValidationContext) {
        request.entries.forEach { entry ->
            val creditAccount = context.transactionContext.accounts[entry.creditAccount]
                ?: throw RuntimeException("Unable to find credit account with code: ${entry.creditAccount}")
            
            val debitAccount = context.transactionContext.accounts[entry.debitAccount]
                ?: throw RuntimeException("Unable to find debit account with code: ${entry.debitAccount}")
            
            if (creditAccount.currencyCode != debitAccount.currencyCode) {
                throw RuntimeException("Currency mismatch between credit account (${creditAccount.currencyCode}) and debit account (${debitAccount.currencyCode})")
            }
            
            if (creditAccount.currencyCode != entry.amount.currency.currencyCode) {
                throw RuntimeException("Currency mismatch between accounts (${creditAccount.currencyCode}) and transaction amount (${entry.amount.currency.currencyCode})")
            }
        }
    }
}

class TransactionLimitValidator : TransactionValidator() {
    override fun doValidate(request: CreateTransactionRequest, context: ValidationContext) {
        request.limit?.let { limit ->
            request.entries.forEach { entry ->
                if (entry.metadata["skip_limits"]?.toBoolean() == true) return@forEach
                
                if (entry.metadata["type"] == MiniglConstants.EntryTypes.AMOUNT) {
                    if (entry.amount.isGreaterThan(limit.maxTransactionDebit)) {
                        throw TransactionLimitExceededException("Maximum transaction debit exceeded: ${entry.amount} > ${limit.maxTransactionDebit}")
                    }
                    
                    if (entry.amount.isLessThan(limit.minTransactionDebit)) {
                        throw TransactionLimitExceededException("Minimum transaction debit not met: ${entry.amount} < ${limit.minTransactionDebit}")
                    }
                }
            }
        }
    }
}

class AccountExistenceValidator : TransactionValidator() {
    override fun doValidate(request: CreateTransactionRequest, context: ValidationContext) {
        val accountCodes = request.entries.flatMap { listOf(it.creditAccount, it.debitAccount) }.toSet()
        
        accountCodes.forEach { accountCode ->
            if (accountCode !in context.transactionContext.accounts) {
                throw RuntimeException("Account not found: $accountCode")
            }
        }
    }
}

class DoubleEntryBalanceValidator : TransactionValidator() {
    override fun doValidate(request: CreateTransactionRequest, context: ValidationContext) {
        val totalDebits = request.entries.sumOf { it.amount.number.numberValue(java.math.BigDecimal::class.java) }
        val totalCredits = request.entries.sumOf { it.amount.number.numberValue(java.math.BigDecimal::class.java) }
        
        if (totalDebits != totalCredits) {
            throw RuntimeException("Transaction does not balance: debits=$totalDebits, credits=$totalCredits")
        }
    }
}