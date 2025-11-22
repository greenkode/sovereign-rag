package ai.sovereignrag.core.content.command

import an.awesome.pipelinr.Command
import ai.sovereignrag.content.dto.IngestResponse
import org.springframework.web.multipart.MultipartFile

data class IngestFileCommand(
    val file: MultipartFile,
    val title: String? = null,
    val url: String? = null
) : Command<IngestResponse>
