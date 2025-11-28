package ai.sovereignrag.fileupload.domain

data class UploadResult(
    val key: String,
    val bucket: String,
    val url: String,
    val contentType: String,
    val size: Long
)

data class PresignedUrlResult(
    val uploadUrl: String,
    val key: String,
    val expiresIn: Long
)

enum class FileCategory(val folder: String) {
    AVATAR("avatars"),
    DOCUMENT("documents"),
    KNOWLEDGE_BASE("knowledge-bases"),
    AGENT("agents")
}
