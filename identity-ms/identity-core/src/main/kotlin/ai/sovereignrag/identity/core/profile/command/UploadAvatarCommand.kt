package ai.sovereignrag.identity.core.profile.command

import an.awesome.pipelinr.Command
import java.io.InputStream

data class UploadAvatarCommand(
    val inputStream: InputStream,
    val fileName: String,
    val contentType: String,
    val size: Long
) : Command<UploadAvatarResult>

data class UploadAvatarResult(
    val success: Boolean,
    val message: String,
    val pictureUrl: String?
)
