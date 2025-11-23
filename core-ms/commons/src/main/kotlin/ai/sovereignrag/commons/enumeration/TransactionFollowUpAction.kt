package ai.sovereignrag.commons.enumeration

import ai.sovereignrag.commons.process.enumeration.ProcessState


enum class TransactionFollowUpAction(val processState: ProcessState) {
    REVERSE(ProcessState.FAILED),
    RECONCILE(ProcessState.UNKNOWN),
    RE_CHECK(ProcessState.PENDING),
    COMPLETE(ProcessState.COMPLETE)
}
