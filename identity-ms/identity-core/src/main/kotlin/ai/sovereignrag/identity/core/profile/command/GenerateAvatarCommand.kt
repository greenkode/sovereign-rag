package ai.sovereignrag.identity.core.profile.command

import ai.sovereignrag.identity.core.profile.dto.AvatarStyle
import an.awesome.pipelinr.Command

data class GenerateAvatarCommand(
    val style: AvatarStyle,
    val backgroundColor: String?
) : Command<GenerateAvatarResult>

data class GenerateAvatarResult(
    val success: Boolean,
    val message: String,
    val pictureUrl: String?
)
