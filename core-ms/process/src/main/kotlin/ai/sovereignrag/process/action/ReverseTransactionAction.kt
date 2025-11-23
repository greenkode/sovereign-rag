package ai.sovereignrag.process.action

import ai.sovereignrag.commons.accounting.TransactionGateway
import ai.sovereignrag.commons.process.ProcessDto
import ai.sovereignrag.commons.process.ProcessGateway
import io.github.oshai.kotlinlogging.KotlinLogging.logger
import jakarta.transaction.Transactional
import org.springframework.stereotype.Component

@Component
@Transactional
class ReverseTransactionAction(
    private val processGateway: ProcessGateway,
    private val transactionGateway: TransactionGateway
) {

    private val log = logger {}

    fun execute(process: ProcessDto) {

        log.info { "Reversing pending transaction: ${process.publicId}" }

        transactionGateway.reverseTransaction(process.publicId)

        processGateway.failProcess(process.publicId)
    }
}