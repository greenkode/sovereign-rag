package ai.sovereignrag.identity.core.settings.command

import ai.sovereignrag.identity.core.entity.EnvironmentMode
import ai.sovereignrag.identity.core.settings.dto.EnvironmentStatusResponse
import an.awesome.pipelinr.Command

data class SwitchEnvironmentCommand(
    val environment: EnvironmentMode
) : Command<EnvironmentStatusResponse>
