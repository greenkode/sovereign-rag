package ai.sovereignrag.process.service

import ai.sovereignrag.commons.property.SystemPropertyScope
import ai.sovereignrag.commons.process.MakeProcessRequestPayload
import ai.sovereignrag.commons.process.ProcessDto
import ai.sovereignrag.commons.property.SystemPropertyGateway
import ai.sovereignrag.commons.process.enumeration.ProcessEvent
import ai.sovereignrag.commons.process.enumeration.ProcessRequestType
import ai.sovereignrag.commons.process.enumeration.ProcessStakeholderType
import ai.sovereignrag.commons.property.SystemPropertyName
import ai.sovereignrag.commons.scheduler.SchedulerGateway
import ai.sovereignrag.commons.transaction.TransactionStatusRetryEvent
import ai.sovereignrag.commons.util.CurrentRequestUtils
import ai.sovereignrag.process.action.CompleteTransactionAction
import ai.sovereignrag.process.action.InitiatePendingTransactionAction
import ai.sovereignrag.process.action.ReverseTransactionAction
import ai.sovereignrag.process.spi.ProcessService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

/**
 * Service handling transaction-specific business logic (bill payments, etc.)
 */
@Service
class TransactionProcessService(
    private val initiatePendingTransactionAction: InitiatePendingTransactionAction,
    private val completePendingTransactionAction: CompleteTransactionAction,
    private val reverseTransactionAction: ReverseTransactionAction,
    private val processService: ProcessService,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val schedulerGateway: SchedulerGateway,
    private val systemPropertyGateway: SystemPropertyGateway
) {

    private val log = KotlinLogging.logger {}

    fun initiatePendingTransaction(process: ProcessDto) {

        log.info { "Initiating pending transaction for process: ${process.publicId}" }

        initiatePendingTransactionAction.execute(process)
    }

    fun completePendingTransaction(process: ProcessDto) {

        log.info { "Completing pending transaction for process: ${process.publicId}" }

        completePendingTransactionAction.execute(process)
    }

    fun reversePendingTransaction(process: ProcessDto) {

        log.info { "Reversing pending transaction for process: ${process.publicId}" }

        reverseTransactionAction.execute(process)
    }

    @Transactional
    fun rescheduleStatusCheck(process: ProcessDto) {

        log.info { "Rescheduling status check for process: ${process.publicId}" }

        val retryPeriod = systemPropertyGateway.findByNameAndScope(
            SystemPropertyName.TRANSACTION_AUTO_RETRY_PERIOD,
            SystemPropertyScope.TRANSACTION
        )?.value?.toLongOrNull() ?: 86400L

        if (process.createdDate.isBefore(Instant.now().minusSeconds(retryPeriod))) {

            processService.makeRequest(
                MakeProcessRequestPayload(
                    UUID.fromString(process.getInitialRequest().getStakeholderValue(ProcessStakeholderType.ACTOR_USER)),
                    process.publicId,
                    ProcessEvent.MANUAL_RECONCILIATION_CONFIRMED,
                    ProcessRequestType.MANUAL_RECONCILIATION,
                    CurrentRequestUtils.getChannel(),
                )
            )
        }

        process.requests.firstOrNull { it.type == ProcessRequestType.STATUS_CHECK_RETRY } ?: run {
            applicationEventPublisher.publishEvent(TransactionStatusRetryEvent(process))
        }
    }

    fun markTransactionForManualReconciliation(process: ProcessDto) {

        log.info { "Marking transaction for manual reconciliation for process: ${process.publicId}" }

        schedulerGateway.deleteJobById(process.publicId.toString(), "pending-transaction-status-check")
    }

    fun handleTransactionExpiry(process: ProcessDto) {
        log.info { "Handling transaction expiry for process: ${process.publicId}" }

        processService.expireProcess(process.publicId, false)
    }
}