package ai.sovereignrag.identity.fileupload.service

import ai.sovereignrag.identity.fileupload.config.S3ConfigProperties
import ai.sovereignrag.identity.fileupload.domain.FileCategory
import ai.sovereignrag.identity.fileupload.domain.PresignedUrlResult
import ai.sovereignrag.identity.fileupload.domain.UploadResult
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.HeadBucketRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.io.InputStream
import java.time.Duration
import java.util.UUID

private val log = KotlinLogging.logger {}

@Service
class FileUploadService(
    private val s3Client: S3Client,
    private val s3Presigner: S3Presigner,
    private val properties: S3ConfigProperties
) {

    fun uploadFile(
        file: MultipartFile,
        category: FileCategory,
        tenantId: UUID,
        customFileName: String? = null
    ): UploadResult {
        val fileName = customFileName ?: generateFileName(file.originalFilename ?: "file")
        val key = buildKey(category, tenantId, fileName)
        val contentType = file.contentType ?: "application/octet-stream"

        log.info { "Uploading file: $key to bucket: ${properties.bucket}" }

        val request = PutObjectRequest.builder()
            .bucket(properties.bucket)
            .key(key)
            .contentType(contentType)
            .contentLength(file.size)
            .build()

        s3Client.putObject(request, RequestBody.fromInputStream(file.inputStream, file.size))

        val url = buildPublicUrl(key)

        log.info { "File uploaded successfully: $key" }

        return UploadResult(
            key = key,
            bucket = properties.bucket,
            url = url,
            contentType = contentType,
            size = file.size
        )
    }

    fun uploadBytes(
        bytes: ByteArray,
        fileName: String,
        contentType: String,
        category: FileCategory,
        tenantId: UUID
    ): UploadResult {
        val key = buildKey(category, tenantId, fileName)

        log.info { "Uploading bytes: $key to bucket: ${properties.bucket}" }

        val request = PutObjectRequest.builder()
            .bucket(properties.bucket)
            .key(key)
            .contentType(contentType)
            .contentLength(bytes.size.toLong())
            .build()

        s3Client.putObject(request, RequestBody.fromBytes(bytes))

        val url = buildPublicUrl(key)

        log.info { "Bytes uploaded successfully: $key" }

        return UploadResult(
            key = key,
            bucket = properties.bucket,
            url = url,
            contentType = contentType,
            size = bytes.size.toLong()
        )
    }

    fun generatePresignedUploadUrl(
        fileName: String,
        contentType: String,
        category: FileCategory,
        tenantId: UUID,
        expirationMinutes: Long = 15
    ): PresignedUrlResult {
        val key = buildKey(category, tenantId, generateFileName(fileName))

        val putObjectRequest = PutObjectRequest.builder()
            .bucket(properties.bucket)
            .key(key)
            .contentType(contentType)
            .build()

        val presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(expirationMinutes))
            .putObjectRequest(putObjectRequest)
            .build()

        val presignedRequest = s3Presigner.presignPutObject(presignRequest)

        return PresignedUrlResult(
            uploadUrl = presignedRequest.url().toString(),
            key = key,
            expiresIn = expirationMinutes * 60
        )
    }

    fun generatePresignedDownloadUrl(key: String, expirationMinutes: Long = 60): String {
        val getObjectRequest = GetObjectRequest.builder()
            .bucket(properties.bucket)
            .key(key)
            .build()

        val presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(expirationMinutes))
            .getObjectRequest(getObjectRequest)
            .build()

        return s3Presigner.presignGetObject(presignRequest).url().toString()
    }

    fun getFileStream(key: String): InputStream {
        val request = GetObjectRequest.builder()
            .bucket(properties.bucket)
            .key(key)
            .build()

        return s3Client.getObject(request)
    }

    fun deleteFile(key: String) {
        log.info { "Deleting file: $key from bucket: ${properties.bucket}" }

        val request = DeleteObjectRequest.builder()
            .bucket(properties.bucket)
            .key(key)
            .build()

        s3Client.deleteObject(request)

        log.info { "File deleted successfully: $key" }
    }

    fun fileExists(key: String): Boolean {
        return runCatching {
            val request = GetObjectRequest.builder()
                .bucket(properties.bucket)
                .key(key)
                .build()
            s3Client.headObject { it.bucket(properties.bucket).key(key) }
            true
        }.getOrElse { false }
    }

    fun bucketExists(): Boolean {
        return runCatching {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(properties.bucket).build())
            true
        }.getOrElse { false }
    }

    private fun buildKey(category: FileCategory, tenantId: UUID, fileName: String): String {
        return "${category.folder}/$tenantId/$fileName"
    }

    private fun buildPublicUrl(key: String): String {
        val baseUrl = properties.publicUrl ?: properties.endpoint
        return "$baseUrl/${properties.bucket}/$key"
    }

    private fun generateFileName(originalName: String): String {
        val extension = originalName.substringAfterLast('.', "")
        val uuid = UUID.randomUUID().toString()
        return if (extension.isNotEmpty()) "$uuid.$extension" else uuid
    }
}
