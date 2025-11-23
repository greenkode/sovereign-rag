package ai.sovereignrag.identity.core.settings.command

import ai.sovereignrag.identity.core.entity.EnvironmentMode
import an.awesome.pipelinr.Command

class ResetKeysCommand(
    val environment: EnvironmentMode
) : Command<ResetKeysResult>

data class ResetKeysResult(
    val clientId: String,
    val clientSecret: String,
    val environment: EnvironmentMode,
    val success: Boolean,
    val message: String
)