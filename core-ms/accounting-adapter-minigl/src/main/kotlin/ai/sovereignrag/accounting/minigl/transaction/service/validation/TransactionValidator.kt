package ai.sovereignrag.accounting.minigl.transaction.service.validation

import ai.sovereignrag.accounting.gateway.api.request.CreateTransactionRequest
import ai.sovereignrag.accounting.minigl.transaction.service.builder.TransactionContext
import org.springframework.stereotype.Component

abstract class TransactionValidator {
    private var next: TransactionValidator? = null
    
    fun setNext(validator: TransactionValidator): TransactionValidator {
        next = validator
        return validator
    }
    
    fun validate(request: CreateTransactionRequest, context: ValidationContext) {
        doValidate(request, context)
        next?.validate(request, context)
    }
    
    protected abstract fun doValidate(request: CreateTransactionRequest, context: ValidationContext)
}

data class ValidationContext(
    val transactionContext: TransactionContext,
    val limits: Map<String, Any> = emptyMap(),
    val additionalData: Map<String, Any> = emptyMap()
)

@Component
class CompositeTransactionValidator(
    private val validators: List<TransactionValidator>
) {
    fun validate(request: CreateTransactionRequest, context: ValidationContext) {

        if (validators.isNotEmpty()) {
            val chainRoot = validators.first()
            validators.drop(1).fold(chainRoot) { current, next ->
                current.setNext(next)
            }
            chainRoot.validate(request, context)
        }
    }
}

class ValidationChainBuilder {
    private val validators = mutableListOf<TransactionValidator>()
    
    fun add(validator: TransactionValidator): ValidationChainBuilder {
        validators.add(validator)
        return this
    }
    
    fun build(): CompositeTransactionValidator {
        return CompositeTransactionValidator(validators.toList())
    }
}