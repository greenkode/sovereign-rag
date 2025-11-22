package ai.sovereignrag.core.content.command

import an.awesome.pipelinr.Command
import ai.sovereignrag.content.dto.DeleteDocumentResponse

data class DeleteDocumentCommand(
    val url: String
) : Command<DeleteDocumentResponse>
