package ai.sovereignrag.process.action

import ai.sovereignrag.commons.accounting.TransactionGateway
import ai.sovereignrag.commons.process.ProcessDto
import ai.sovereignrag.commons.process.ProcessGateway
import ai.sovereignrag.commons.process.enumeration.ProcessRequestDataName
import io.github.oshai.kotlinlogging.KotlinLogging.logger
import jakarta.transaction.Transactional
import org.springframework.stereotype.Component

@Component
class CompleteTransactionAction(
    private val processGateway: ProcessGateway,
    private val transactionGateway: TransactionGateway
) {

    private val log = logger {}

    @Transactional
    fun execute(process: ProcessDto) {

        val request = process.getInitialRequest()

        log.info { "Completing transaction for process ${process.publicId}" }

        request.getDataValueOrNull(ProcessRequestDataName.EXTERNAL_REFERENCE)?.let {

            transactionGateway.completePendingTransaction(it)

        } ?: transactionGateway.completePendingTransaction(process.publicId)

        processGateway.completeProcess(process.publicId, request.id)
    }
}
