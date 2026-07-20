package xyz.om3lette.deadlines_api.services.storage

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.status
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import xyz.om3lette.deadlines_api.configs.properties.AttachmentsProperties
import xyz.om3lette.deadlines_api.configs.properties.StorageProperties
import xyz.om3lette.deadlines_api.data.attachments.enums.AttachmentDisposition
import xyz.om3lette.deadlines_api.data.attachments.model.Attachment
import xyz.om3lette.deadlines_api.data.attachments.repo.AttachmentRepository
import xyz.om3lette.deadlines_api.data.attachments.reponse.AttachmentCreatedResponse
import xyz.om3lette.deadlines_api.data.attachments.reponse.AttachmentResponse
import xyz.om3lette.deadlines_api.data.permissions.dto.DeadlineScope
import xyz.om3lette.deadlines_api.data.scopes.deadline.repo.DeadlineRepository
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.exceptions.enums.ErrorCode
import xyz.om3lette.deadlines_api.exceptions.type.StatusCodeException
import xyz.om3lette.deadlines_api.services.permission.PermissionService
import xyz.om3lette.deadlines_api.util.jpaRepository.findByIdOr404
import xyz.om3lette.deadlines_api.util.requirePermission
import java.net.URI
import java.time.Instant
import java.util.*

@Service
class AttachmentsService (
    private val s3Client: S3Client,
    private val s3Presigner: S3Presigner,
    storageProperties: StorageProperties,
    attachmentsProperties: AttachmentsProperties,
    private val permissionService: PermissionService,
    private val attachmentRepository: AttachmentRepository,
    private val deadlineRepository: DeadlineRepository,
    private val fileCheckerService: FileCheckerService
) {
    private val logger = LoggerFactory.getLogger(AttachmentsService::class.java)
    private val s3Properties = storageProperties.s3
    private val maxAttachmentsPerDeadline = attachmentsProperties.maxPerDeadline

    fun createAttachment(
        issuer: User,
        deadlineId: Long,
        fileStream: MultipartFile,
        filename: String
    ): AttachmentCreatedResponse {
        val deadline = deadlineRepository.findByIdOr404(deadlineId, ErrorCode.DDL_NOT_FOUND)
        requirePermission(
            permissionService.canManageDeadlineAttachments(issuer, deadline)
        )
        if (attachmentRepository.countByDeadline(deadline) >= maxAttachmentsPerDeadline) {
            throw StatusCodeException(409, ErrorCode.ATTACHMENT_LIMIT_EXCEEDED)
        }

        val mimeType = fileCheckerService.getAttachmentMimeTypeOr403(fileStream)
        val objectKey = UUID.randomUUID().toString()

        try {
            putObject(objectKey, fileStream, mimeType)

            val attachment = attachmentRepository.save(
                Attachment(
                    0,
                    objectKey,
                    filename,
                    mimeType,
                    fileStream.size,
                    issuer,
                    deadline,
                    Instant.now()
                )
            )
            return AttachmentCreatedResponse(attachment.id)
        } catch (e: Exception) {
            runCatching {
                deleteObject(objectKey)
            }
            logger.error("Attachment upload failed: $e")
            throw StatusCodeException(500, ErrorCode.ATTACHMENT_UPLOAD_FAILED)
        }
    }

    fun replaceAttachment(issuer: User, attachmentId: Long, fileStream: MultipartFile) {
        val attachment = attachmentRepository.findByIdOr404(attachmentId, ErrorCode.ATTACHMENT_NOT_FOUND)

        // Avoid a db request by first validating the fileStream
        val mimeType = fileCheckerService.getAttachmentMimeTypeOr403(fileStream)
        requirePermission(
            permissionService.canManageDeadlineAttachments(issuer, attachment.deadline)
        )

        try {
            putObject(attachment.objectKey, fileStream, mimeType)

            attachment.uploadedAt = Instant.now()
            attachment.mimeType = mimeType
            attachment.sizeBytes = fileStream.size

            attachmentRepository.save(attachment)
        } catch (_: Exception) {
            throw StatusCodeException(500, ErrorCode.ATTACHMENT_UPLOAD_FAILED)
        }
    }

    fun patchAttachmentMetadata(issuer: User, attachmentId: Long, filename: String?) {
        if (filename == null) {
            return
        }
        val attachment = attachmentRepository.findByIdOr404(attachmentId, ErrorCode.ATTACHMENT_NOT_FOUND)
        requirePermission(
            permissionService.canManageDeadlineAttachments(issuer, attachment.deadline)
        )

        attachment.filename = filename // Check for null if new metadata is added
        attachmentRepository.save(attachment)
    }

    @Transactional
    fun deleteAttachment(issuer: User, attachmentId: Long) {
        val attachment = attachmentRepository.findByIdOr404(attachmentId, ErrorCode.ATTACHMENT_NOT_FOUND)

        requirePermission(
            permissionService.canManageDeadlineAttachments(issuer, attachment.deadline)
        )

        try {
            attachmentRepository.delete(attachment)
            // TODO: Requires a scheduled job of sorts to ensure that deleteObject does not fail
            deleteObject(attachment.objectKey)
        } catch (_: Exception) {
            throw StatusCodeException(500, ErrorCode.ATTACHMENT_UPLOAD_FAILED)
        }
    }

    fun getAttachment(issuer: User, attachmentId: Long, disposition: String?): ResponseEntity<Void> {
        val attachment = getAttachmentByIdAndCheckPermissions(issuer, attachmentId)
        val presignedUrl = presignGetObjectUrl(attachment, AttachmentDisposition.from(disposition))

        return status(HttpStatus.FOUND)
            .location(URI.create(presignedUrl))
            .build()
    }

    fun getAttachmentMetadata(issuer: User, attachmentId: Long): AttachmentResponse {
        val attachment = getAttachmentByIdAndCheckPermissions(issuer, attachmentId)
        return attachment.toResponse(
            permissionService.buildDeadlineAttachmentPermissions(issuer, attachment)
        )
    }

    fun getDeadlineAttachmentsMetadata(
        issuer: User,
        deadlineId: Long
    ): List<AttachmentResponse> {
        val deadline = deadlineRepository.findByIdOr404(deadlineId, ErrorCode.DDL_NOT_FOUND)
        requirePermission(
            permissionService.hasAccess(issuer, DeadlineScope(deadline))
        )
        // There is no pagination as it is logical frontend wise to fetch all the metadata at once,
        // while the `maxAttachmentsPerDeadline` insures that the response has an acceptable size
        return attachmentRepository.findAllByDeadlineOrderByUploadedAtDesc(deadline).map { it.toResponse(
            permissionService.buildDeadlineAttachmentPermissions(issuer, it)
        ) }
    }


    private fun getAttachmentByIdAndCheckPermissions(issuer: User, attachmentId: Long): Attachment {
        val attachment = attachmentRepository.findByIdOr404(attachmentId, ErrorCode.ATTACHMENT_NOT_FOUND)
        val deadline = attachment.deadline
        requirePermission(
            permissionService.hasAccess(issuer, DeadlineScope(deadline))
        )
        return attachment
    }

    private fun putObject(objectKey: String, fileStream: MultipartFile, mimeType: String) {
        val request = PutObjectRequest.builder()
            .bucket(s3Properties.bucket)
            .key(objectKey)
            .contentType(mimeType)
            .contentLength(fileStream.size)
            .build()

        s3Client.putObject(
            request,
            RequestBody.fromContentProvider(
                { fileStream.inputStream },
                fileStream.size,
                mimeType
            )
        )
    }

    private fun deleteObject(objectKey: String) {
        s3Client.deleteObject(
            DeleteObjectRequest.builder()
                .bucket(s3Properties.bucket)
                .key(objectKey)
                .build()
        )
    }

    private fun presignGetObjectUrl(attachment: Attachment, disposition: AttachmentDisposition): String {
        val getObjectRequest = GetObjectRequest.builder()
            .bucket(s3Properties.bucket)
            .key(attachment.objectKey)
            .responseContentType(attachment.mimeType)
            .responseContentDisposition(
                "${disposition.headerValue}; filename=\"${attachment.filename.contentDispositionEscaped()}\""
            )
            .build()
        val presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(s3Properties.presignedUrlExpiration)
            .getObjectRequest(getObjectRequest)
            .build()

        return s3Presigner.presignGetObject(presignRequest).url().toString()
    }

    private fun String.contentDispositionEscaped(): String =
        replace("\\", "\\\\").replace("\"", "\\\"")


}
