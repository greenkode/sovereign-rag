package ai.sovereignrag.identity.core.admin.command

import an.awesome.pipelinr.Command

data class UnlockAccountCommand(
    val identifier: String,
    val isUser: Boolean
) : Command<UnlockAccountResult>

data class UnlockAccountResult(
    val status: String,
    val message: String,
    val identifier: String,
    val isUser: Boolean
)
