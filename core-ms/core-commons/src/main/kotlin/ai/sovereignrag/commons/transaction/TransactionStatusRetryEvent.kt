package ai.sovereignrag.commons.transaction

import ai.sovereignrag.commons.process.ProcessDto


data class TransactionStatusRetryEvent(val process: ProcessDto)