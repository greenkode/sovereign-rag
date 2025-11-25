package ai.sovereignrag.identity.core.admin.query

import an.awesome.pipelinr.Command

data class GetLockoutStatusQuery(
    val identifier: String,
    val isUser: Boolean
) : Command<LockoutStatusResult>

data class LockoutStatusResult(
    val identifier: String,
    val locked: Boolean,
    val remainingMinutes: Long?,
    val message: String
)
