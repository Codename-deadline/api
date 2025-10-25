package xyz.om3lette.deadlines_api.services.storage

import io.minio.MinioClient
import org.slf4j.LoggerFactory
import org.springframework.core.io.InputStreamResource
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import xyz.om3lette.deadlines_api.data.attachments.model.Attachment
import xyz.om3lette.deadlines_api.data.attachments.repo.AttachmentRepository
import xyz.om3lette.deadlines_api.data.attachments.reponse.AttachmentCreatedResponse
import xyz.om3lette.deadlines_api.data.attachments.reponse.AttachmentResponse
import xyz.om3lette.deadlines_api.data.scopes.deadline.repo.DeadlineRepository
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.exceptions.type.StatusCodeException
import xyz.om3lette.deadlines_api.services.permission.PermissionLookupService
import xyz.om3lette.deadlines_api.services.permission.PermissionService
import xyz.om3lette.deadlines_api.util.jpaRepository.findByIdOr404
import xyz.om3lette.deadlines_api.util.minioClient.getObject
import xyz.om3lette.deadlines_api.util.minioClient.putObject
import xyz.om3lette.deadlines_api.util.minioClient.removeObject
import xyz.om3lette.deadlines_api.util.requirePermission
import java.time.Instant
import java.util.UUID

@Service
class AttachmentsService (
    private val minioClient: MinioClient,
    private val bucketName: String = "attachments", // TODO: Replace with @Value?
    private val permissionLookupService: PermissionLookupService,
    private val permissionService: PermissionService,
    private val attachmentRepository: AttachmentRepository,
    private val deadlineRepository: DeadlineRepository,
    private val fileCheckerService: FileCheckerService
) {
    private val logger = LoggerFactory.getLogger(AttachmentsService::class.java)

    fun createAttachment(
        issuer: User,
        deadlineId: Long,
        fileStream: MultipartFile,
        filename: String
    ): AttachmentCreatedResponse {
        val (deadline, issuerScope) = permissionLookupService.getDeadlineAndHighestRoleUserScopeOr404(issuer, deadlineId)
        requirePermission(
            permissionService.canManageDeadlineAttachments(issuer, issuerScope)
        )

        val (mimeType, attachmentType) = fileCheckerService.getAttachmentTypeOr403(fileStream)
        val objectKey = UUID.randomUUID().toString()

        try {
            minioClient.putObject(bucketName, objectKey) {
                stream(fileStream.inputStream, fileStream.size, -1)
                contentType(mimeType)
            }

            val attachment = attachmentRepository.save(
                Attachment(
                    0,
                    objectKey,
                    filename,
                    attachmentType,
                    issuer,
                    deadline,
                    Instant.now()
                )
            )
            return AttachmentCreatedResponse(attachment.id)
        } catch (e: Exception) {
            runCatching {
                minioClient.removeObject(bucketName, objectKey)
            }
            logger.error("Attachment upload failed: $e")
            throw StatusCodeException(500, "Attachment upload failed")
        }
    }

    fun replaceAttachment(issuer: User, attachmentId: Long, fileStream: MultipartFile, filename: String?) {
        val attachment = attachmentRepository.findByIdOr404(attachmentId)
//      Avoid a db request by first validating the fileStream
        val (mimeType, newAttachmentType) = fileCheckerService.getAttachmentTypeOr403(fileStream)
        val issuerScope = permissionLookupService.getHighestRoleUserScopeOr404(issuer, attachment.deadline)
        requirePermission(
            permissionService.canManageDeadlineAttachments(issuer, issuerScope)
        )

        try {
            minioClient.putObject(bucketName, attachment.objectKey) {
                stream(fileStream.inputStream, fileStream.size, -1)
                contentType(mimeType)
            }

            attachment.uploadedAt = Instant.now()
            attachment.type = newAttachmentType
            if (filename != null) attachment.filename = filename

            attachmentRepository.save(attachment)
        } catch (_: Exception) {
            throw StatusCodeException(500, "Attachment update failed")
        }
    }

    fun patchAttachmentMetadata(issuer: User, attachmentId: Long, filename: String?) {
        if (filename == null) {
            return
        }
        val attachment = attachmentRepository.findByIdOr404(attachmentId)
        val issuerScope = permissionLookupService.getHighestRoleUserScopeOr404(issuer, attachment.deadline)
        requirePermission(
            permissionService.canManageDeadlineAttachments(issuer, issuerScope)
        )

        attachment.filename = filename // Check for null if new metadata is added
        attachmentRepository.save(attachment)
    }

    fun deleteAttachment(issuer: User, attachmentId: Long) {
        val attachment = attachmentRepository.findByIdOr404(attachmentId)
        val issuerScope = permissionLookupService.getHighestRoleUserScopeOr404(issuer, attachment.deadline)
        requirePermission(
            permissionService.canManageDeadlineAttachments(issuer, issuerScope)
        )

        try {
            minioClient.removeObject(bucketName, attachment.objectKey)
//          FIXME: Potential orphan db entries if `delete` fails
            attachmentRepository.delete(attachment)
        } catch (_: Exception) {
            throw StatusCodeException(500, "Attachment removal failed")
        }
    }

    fun getAttachment(issuer: User, attachmentId: Long): ResponseEntity<InputStreamResource> {
        val attachment = getAttachmentByIdAndCheckPermissions(issuer, attachmentId)
        val getObjectResponse = minioClient.getObject(bucketName, attachment.objectKey)

        val headers = getObjectResponse.headers()

        val contentType = MediaType.parseMediaType(headers["Content-Type"] ?: MediaType.ALL_VALUE)
        val length = headers["Content-Length"]?.toLong() ?: -1L
        val resource = InputStreamResource(getObjectResponse)

        return ResponseEntity.ok()
            .contentType(contentType)
            .contentLength(length)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${attachment.filename}\"")
            .body(resource)
    }

    fun getAttachmentMetadata(issuer: User, attachmentId: Long): AttachmentResponse =
        getAttachmentByIdAndCheckPermissions(issuer, attachmentId).toResponse()

    fun getDeadlineAttachmentsMetadata(
        issuer: User,
        deadlineId: Long,
        pageNumber: Int,
        pageSize: Int
    ): List<AttachmentResponse> {
        val deadline = deadlineRepository.findByIdOr404(deadlineId)
        requirePermission(
            permissionService.hasDeadlineAccess(
                issuer,
                permissionLookupService.getHighestRoleUserScopeOr404(issuer, deadline),
                deadline.organization
            )
        )
        return attachmentRepository.findAllByDeadline(
            deadline,
            PageRequest.of(pageNumber, pageSize)
        ).map { it.toResponse() }
    }


    private fun getAttachmentByIdAndCheckPermissions(issuer: User, attachmentId: Long): Attachment {
        val attachment = attachmentRepository.findByIdOr404(attachmentId)
        val deadline = attachment.deadline
        requirePermission(
            permissionService.hasDeadlineAccess(
                issuer,
                permissionLookupService.getHighestRoleUserScopeOr404(issuer, deadline),
                deadline.organization
            )
        )
        return attachment
    }


}