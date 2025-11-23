package ai.sovereignrag.process.service

import ai.sovereignrag.commons.process.ProcessDto
import ai.sovereignrag.process.action.CompleteTransactionAction
import ai.sovereignrag.process.action.InitiatePendingTransactionAction
import ai.sovereignrag.process.action.ReverseTransactionAction
import ai.sovereignrag.process.spi.ProcessService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

/**
 * Service handling deposit-specific business logic
 */
@Service
class DepositProcessService(
    private val pendingTransactionAction: InitiatePendingTransactionAction,
    private val reverseTransactionAction: ReverseTransactionAction,
    private val completePendingTransactionAction: CompleteTransactionAction,
    private val processService: ProcessService
) {

    private val log = KotlinLogging.logger {}

    fun initiateDeposit(process: ProcessDto) {

        pendingTransactionAction.execute(process)
    }

    fun completeDeposit(process: ProcessDto) {

        log.info { "Completing deposit for process: ${process.publicId}" }

        completePendingTransactionAction.execute(process)
    }

    fun handleDepositFailure(process: ProcessDto) {
        log.info { "Handling deposit failure for process: ${process.publicId}" }

        processService.failProcess(process.publicId)
    }

    fun handleDepositExpiry(process: ProcessDto) {

        log.info { "Handling deposit expiry for process: ${process.publicId}" }

        processService.expireProcess(process.publicId, false)
    }


    fun reversePendingTransaction(process: ProcessDto) {

        log.info { "Reversing pending transaction for process: ${process.publicId}" }

        reverseTransactionAction.execute(process)
    }
}