package ai.sovereignrag.process.service

import ai.sovereignrag.process.action.lien.LienAmountAction
import ai.sovereignrag.process.action.lien.UnlienAmountAction
import ai.sovereignrag.accounting.transaction.spi.TransactionService
import ai.sovereignrag.commons.accounting.AccountGateway
import ai.sovereignrag.commons.process.ProcessDto
import ai.sovereignrag.process.spi.ProcessService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

/**
 * Service handling lien-specific business logic
 */
@Service
class LienProcessService(
    private val lienAmountAction: LienAmountAction,
    private val unlienAmountAction: UnlienAmountAction,
    private val processService: ProcessService
) {

    private val log = KotlinLogging.logger {}

    fun lienAmount(process: ProcessDto) {
        lienAmountAction.execute(process)
    }

    fun unlienAmount(process: ProcessDto) {
        unlienAmountAction.execute(process)
    }

    fun handleLienFailure(process: ProcessDto) {
        log.info { "Handling lien failure for process: ${process.publicId}" }
        processService.failProcess(process.publicId)
    }

    fun handleLienExpiry(process: ProcessDto) {
        log.info { "Handling lien expiry for process: ${process.publicId}" }
        processService.expireProcess(process.publicId, false)
    }
}