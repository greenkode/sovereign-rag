package ai.sovereignrag.ingestion.core.gateway

import ai.sovereignrag.commons.fileupload.FileUploadGateway
import ai.sovereignrag.commons.fileupload.FileUploadResult
import ai.sovereignrag.commons.fileupload.PresignedUploadUrlResult
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.io.InputStream
import java.time.Duration
import java.util.UUID

private val log = KotlinLogging.logger {}

@Component
class S3FileUploadGateway(
    private val s3Client: S3Client,
    private val s3Presigner: S3Presigner,
    @Qualifier("storageBucket") private val bucket: String,
    @Qualifier("storageBaseUrl") private val baseUrl: String
) : FileUploadGateway {

    override fun uploadFile(
        inputStream: InputStream,
        fileName: String,
        contentType: String,
        size: Long,
        category: String,
        ownerId: UUID
    ): FileUploadResult {
        val generatedFileName = generateFileName(fileName)
        val key = buildKey(category, ownerId, generatedFileName)

        log.info { "Uploading file: $key to bucket: $bucket" }

        val request = PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .contentType(contentType)
            .contentLength(size)
            .build()

        s3Client.putObject(request, RequestBody.fromInputStream(inputStream, size))

        val url = buildPublicUrl(key)

        log.info { "File uploaded successfully: $key" }

        return FileUploadResult(
            key = key,
            url = url,
            contentType = contentType,
            size = size
        )
    }

    override fun uploadBytes(
        bytes: ByteArray,
        fileName: String,
        contentType: String,
        category: String,
        ownerId: UUID
    ): FileUploadResult {
        val generatedFileName = generateFileName(fileName)
        val key = buildKey(category, ownerId, generatedFileName)

        log.info { "Uploading bytes: $key to bucket: $bucket" }

        val request = PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .contentType(contentType)
            .contentLength(bytes.size.toLong())
            .build()

        s3Client.putObject(request, RequestBody.fromBytes(bytes))

        val url = buildPublicUrl(key)

        log.info { "Bytes uploaded successfully: $key" }

        return FileUploadResult(
            key = key,
            url = url,
            contentType = contentType,
            size = bytes.size.toLong()
        )
    }

    override fun generatePresignedUploadUrl(
        fileName: String,
        contentType: String,
        category: String,
        ownerId: UUID,
        expirationMinutes: Long
    ): PresignedUploadUrlResult {
        val generatedFileName = generateFileName(fileName)
        val key = buildKey(category, ownerId, generatedFileName)

        val putObjectRequest = PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .contentType(contentType)
            .build()

        val presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(expirationMinutes))
            .putObjectRequest(putObjectRequest)
            .build()

        val presignedRequest = s3Presigner.presignPutObject(presignRequest)

        return PresignedUploadUrlResult(
            uploadUrl = presignedRequest.url().toString(),
            key = key,
            expiresIn = expirationMinutes * 60
        )
    }

    override fun generatePresignedDownloadUrl(key: String, expirationMinutes: Long): String {
        val getObjectRequest = GetObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .build()

        val presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(expirationMinutes))
            .getObjectRequest(getObjectRequest)
            .build()

        return s3Presigner.presignGetObject(presignRequest).url().toString()
    }

    override fun getFileStream(key: String): InputStream {
        val request = GetObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .build()

        return s3Client.getObject(request)
    }

    override fun deleteFile(key: String) {
        log.info { "Deleting file: $key from bucket: $bucket" }

        val request = DeleteObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .build()

        s3Client.deleteObject(request)

        log.info { "File deleted successfully: $key" }
    }

    override fun getPublicUrl(key: String): String {
        return buildPublicUrl(key)
    }

    override fun uploadUserFile(
        inputStream: InputStream,
        fileName: String,
        contentType: String,
        size: Long,
        category: String,
        userId: UUID
    ): FileUploadResult {
        val generatedFileName = generateFileName(fileName)
        val key = buildUserKey(category, userId, generatedFileName)

        log.info { "Uploading user file: $key to bucket: $bucket" }

        val request = PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .contentType(contentType)
            .contentLength(size)
            .build()

        s3Client.putObject(request, RequestBody.fromInputStream(inputStream, size))

        val url = buildPublicUrl(key)

        log.info { "User file uploaded successfully: $key" }

        return FileUploadResult(
            key = key,
            url = url,
            contentType = contentType,
            size = size
        )
    }

    override fun deleteUserFile(category: String, userId: UUID, fileName: String) {
        val key = buildUserKey(category, userId, fileName)
        log.info { "Deleting user file: $key from bucket: $bucket" }

        val request = DeleteObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .build()

        s3Client.deleteObject(request)

        log.info { "User file deleted successfully: $key" }
    }

    private fun buildKey(category: String, ownerId: UUID, fileName: String): String {
        return "$category/$ownerId/$fileName"
    }

    private fun buildUserKey(category: String, userId: UUID, fileName: String): String {
        return "$category/users/$userId/$fileName"
    }

    private fun buildPublicUrl(key: String): String {
        return "$baseUrl/$bucket/$key"
    }

    private fun generateFileName(originalName: String): String {
        val extension = originalName.substringAfterLast('.', "")
        val uuid = UUID.randomUUID().toString()
        return if (extension.isNotEmpty()) "$uuid.$extension" else uuid
    }
}
