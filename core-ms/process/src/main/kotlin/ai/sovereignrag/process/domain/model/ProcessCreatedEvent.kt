package ai.sovereignrag.process.domain.model

import ai.sovereignrag.commons.process.enumeration.ProcessType
import java.util.UUID

data class ProcessCreatedEvent(val id: UUID, val processType: ProcessType, val expiry: Long? = null)