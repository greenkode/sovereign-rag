package ai.sovereignrag.process.action.lien

import ai.sovereignrag.commons.process.enumeration.ProcessState
import java.util.UUID

data class UnlienAmountEvent(val processRequestId: Long, val state: ProcessState, val processPublicId: UUID, val message: String? = null)