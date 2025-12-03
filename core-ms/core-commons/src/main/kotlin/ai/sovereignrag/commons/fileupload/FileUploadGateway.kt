package ai.sovereignrag.commons.fileupload

import java.io.InputStream
import java.util.UUID

interface FileUploadGateway {
    fun uploadFile(
        inputStream: InputStream,
        fileName: String,
        contentType: String,
        size: Long,
        category: String,
        ownerId: UUID
    ): FileUploadResult

    fun uploadUserFile(
        inputStream: InputStream,
        fileName: String,
        contentType: String,
        size: Long,
        category: String,
        userId: UUID
    ): FileUploadResult

    fun uploadBytes(
        bytes: ByteArray,
        fileName: String,
        contentType: String,
        category: String,
        ownerId: UUID
    ): FileUploadResult

    fun generatePresignedUploadUrl(
        fileName: String,
        contentType: String,
        category: String,
        ownerId: UUID,
        expirationMinutes: Long = 15
    ): PresignedUploadUrlResult

    fun generatePresignedDownloadUrl(key: String, expirationMinutes: Long = 60): String

    fun getFileStream(key: String): InputStream

    fun deleteFile(key: String)

    fun getPublicUrl(key: String): String

    fun deleteUserFile(category: String, userId: UUID, fileName: String)
}

data class FileUploadResult(
    val key: String,
    val url: String,
    val contentType: String,
    val size: Long
)

data class PresignedUploadUrlResult(
    val uploadUrl: String,
    val key: String,
    val expiresIn: Long
)
