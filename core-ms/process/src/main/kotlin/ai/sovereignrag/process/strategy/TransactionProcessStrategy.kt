package ai.sovereignrag.process.strategy

import ai.sovereignrag.commons.process.ProcessDto
import ai.sovereignrag.commons.process.enumeration.ProcessEvent
import ai.sovereignrag.commons.process.enumeration.ProcessState
import ai.sovereignrag.commons.process.enumeration.ProcessStrategyBeanNames
import ai.sovereignrag.process.service.TransactionProcessService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

@Component(ProcessStrategyBeanNames.TRANSACTION_PROCESS_STRATEGY)
class TransactionProcessStrategy(
    @Lazy private val transactionProcessService: TransactionProcessService
) : ProcessStrategy {

    private val log = KotlinLogging.logger {}

    override fun processEvent(
        process: ProcessDto,
        event: ProcessEvent
    ): ProcessState {

        log.info { "Processing transaction event: $event for process: ${process.publicId}" }

        return when (process.state to event) {
            ProcessState.PENDING to ProcessEvent.AUTH_SUCCEEDED -> {
                transactionProcessService.initiatePendingTransaction(process)
                ProcessState.PENDING
            }

            ProcessState.PENDING to ProcessEvent.CREDIT_RATING_OFFERS_RECEIVED -> {
                ProcessState.PENDING
            }

            ProcessState.PENDING to ProcessEvent.REMOTE_PAYMENT_RESULT -> {
                ProcessState.PENDING
            }

            ProcessState.PENDING to ProcessEvent.REMOTE_PAYMENT_COMPLETED,
            ProcessState.PENDING to ProcessEvent.PENDING_TRANSACTION_STATUS_VERIFIED,
            ProcessState.PENDING to ProcessEvent.PROCESS_COMPLETED -> {
                transactionProcessService.completePendingTransaction(process)
                ProcessState.COMPLETE
            }

            ProcessState.PENDING to ProcessEvent.STATUS_CHECK_FAILED -> {
                transactionProcessService.rescheduleStatusCheck(process)
                ProcessState.PENDING
            }

            ProcessState.PENDING to ProcessEvent.MANUAL_RECONCILIATION_CONFIRMED -> {
                transactionProcessService.markTransactionForManualReconciliation(process)
                ProcessState.PENDING
            }

            ProcessState.PENDING to ProcessEvent.PROCESS_FAILED,
            ProcessState.PENDING to ProcessEvent.REVERSE_TRANSACTION,
            ProcessState.PENDING to ProcessEvent.REVERSE_PENDING_FUNDS,
            ProcessState.COMPLETE to ProcessEvent.REVERSE_TRANSACTION,
            ProcessState.EXPIRED to ProcessEvent.REVERSE_TRANSACTION -> {
                transactionProcessService.reversePendingTransaction(process)
                ProcessState.FAILED
            }

            ProcessState.PENDING to ProcessEvent.PROCESS_EXPIRED -> {
                transactionProcessService.handleTransactionExpiry(process)
                ProcessState.EXPIRED
            }

            else -> {
                log.warn { "Invalid state transition: ${process.state} -> $event" }
                throw IllegalStateException("Invalid transition from ${process.state} with event $event")
            }
        }
    }

    override fun isValidTransition(
        currentState: ProcessState,
        event: ProcessEvent,
        targetState: ProcessState
    ): Boolean {
        return when (Triple(currentState, event, targetState)) {
            Triple(ProcessState.PENDING, ProcessEvent.AUTH_SUCCEEDED, ProcessState.PENDING) -> true
            Triple(ProcessState.PENDING, ProcessEvent.CREDIT_RATING_OFFERS_RECEIVED, ProcessState.PENDING) -> true
            Triple(ProcessState.PENDING, ProcessEvent.REMOTE_PAYMENT_COMPLETED, ProcessState.COMPLETE) -> true
            Triple(ProcessState.PENDING, ProcessEvent.REMOTE_PAYMENT_RESULT, ProcessState.PENDING) -> true
            Triple(ProcessState.PENDING, ProcessEvent.PENDING_TRANSACTION_STATUS_VERIFIED, ProcessState.COMPLETE) -> true
            Triple(ProcessState.PENDING, ProcessEvent.STATUS_CHECK_FAILED, ProcessState.PENDING) -> true
            Triple(ProcessState.PENDING, ProcessEvent.MANUAL_RECONCILIATION_CONFIRMED, ProcessState.PENDING) -> true
            Triple(ProcessState.PENDING, ProcessEvent.PROCESS_FAILED, ProcessState.FAILED) -> true
            Triple(ProcessState.PENDING, ProcessEvent.PROCESS_EXPIRED, ProcessState.EXPIRED) -> true
            Triple(ProcessState.PENDING, ProcessEvent.PROCESS_COMPLETED, ProcessState.COMPLETE) -> true
            Triple(
                ProcessState.PENDING,
                ProcessEvent.PENDING_TRANSACTION_STATUS_VERIFIED,
                ProcessState.COMPLETE
            ) -> true

            Triple(ProcessState.PENDING, ProcessEvent.REVERSE_TRANSACTION, ProcessState.FAILED) -> true
            Triple(ProcessState.COMPLETE, ProcessEvent.REVERSE_TRANSACTION, ProcessState.FAILED) -> true
            Triple(ProcessState.EXPIRED, ProcessEvent.REVERSE_TRANSACTION, ProcessState.FAILED) -> true
            Triple(ProcessState.PENDING, ProcessEvent.REVERSE_PENDING_FUNDS, ProcessState.FAILED) -> true
            else -> false
        }
    }

    override fun calculateExpectedState(
        currentState: ProcessState,
        event: ProcessEvent
    ): ProcessState {
        return when (currentState to event) {
            ProcessState.PENDING to ProcessEvent.AUTH_SUCCEEDED -> ProcessState.PENDING
            ProcessState.PENDING to ProcessEvent.CREDIT_RATING_OFFERS_RECEIVED -> ProcessState.PENDING
            ProcessState.PENDING to ProcessEvent.REMOTE_PAYMENT_COMPLETED -> ProcessState.COMPLETE
            ProcessState.PENDING to ProcessEvent.PENDING_TRANSACTION_STATUS_VERIFIED -> ProcessState.COMPLETE
            ProcessState.PENDING to ProcessEvent.PROCESS_FAILED -> ProcessState.FAILED
            ProcessState.PENDING to ProcessEvent.PROCESS_EXPIRED -> ProcessState.EXPIRED
            ProcessState.PENDING to ProcessEvent.REVERSE_PENDING_FUNDS -> ProcessState.FAILED
            ProcessState.PENDING to ProcessEvent.REVERSE_TRANSACTION -> ProcessState.FAILED
            ProcessState.COMPLETE to ProcessEvent.REVERSE_TRANSACTION -> ProcessState.FAILED
            ProcessState.EXPIRED to ProcessEvent.REVERSE_TRANSACTION -> ProcessState.FAILED
            else -> currentState // No state change expected
        }
    }
}