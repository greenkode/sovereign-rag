package ai.sovereignrag.core.content.command

import an.awesome.pipelinr.Command
import nl.compilot.ai.content.dto.DeleteDocumentResponse

data class DeleteDocumentCommand(
    val url: String
) : Command<DeleteDocumentResponse>
