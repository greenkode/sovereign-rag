package ai.sovereignrag.commons.billpay

import ai.sovereignrag.commons.process.ProcessDto

data class TransactionStatusRetryEvent(val process: ProcessDto)