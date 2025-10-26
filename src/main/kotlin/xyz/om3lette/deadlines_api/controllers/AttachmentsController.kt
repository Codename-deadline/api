package xyz.om3lette.deadlines_api.controllers

import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import xyz.om3lette.deadlines_api.data.attachments.request.FileMetadata
import xyz.om3lette.deadlines_api.data.attachments.request.PatchFileMetadataRequest
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.services.storage.AttachmentsService

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/attachments")
@Tag(name = "Attachments")
class AttachmentsController(
    private val attachmentsService: AttachmentsService
) {
    @PostMapping("/deadline/{deadlineId}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun createAttachment(
        @AuthenticationPrincipal user: User,
        @PathVariable deadlineId: Long,
        @RequestPart("meta") @Valid meta: FileMetadata,
        @RequestPart("file") file: MultipartFile
    ) = attachmentsService.createAttachment(user, deadlineId, file, meta.filename)

    @PutMapping("/{attachmentId}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun replaceAttachment(
        @AuthenticationPrincipal user: User,
        @PathVariable attachmentId: Long,
        @RequestPart("meta") @Valid meta: FileMetadata,
        @RequestPart("file") file: MultipartFile
    ) = attachmentsService.replaceAttachment(user, attachmentId, file, meta.filename)

    @GetMapping("/{attachmentId}")
    fun getAttachment(
        @AuthenticationPrincipal user: User,
        @PathVariable attachmentId: Long
    ) = attachmentsService.getAttachment(user, attachmentId)

    @DeleteMapping("/{attachmentId}")
    fun deleteAttachmentMetadata(
        @AuthenticationPrincipal user: User,
        @PathVariable attachmentId: Long
    ) = attachmentsService.deleteAttachment(user, attachmentId)

    @GetMapping("/{attachmentId}/metadata")
    fun getAttachmentMetadata(
        @AuthenticationPrincipal user: User,
        @PathVariable attachmentId: Long
    ) = attachmentsService.getAttachmentMetadata(user, attachmentId)

    @GetMapping("/deadline/{deadlineId}")
    fun getDeadlineAttachmentsMetadata(
        @AuthenticationPrincipal user: User,
        @PathVariable deadlineId: Long,
        @RequestParam("page") pageNumber: Int
    ) = attachmentsService.getDeadlineAttachmentsMetadata(user, deadlineId, pageNumber, 10)

    @PatchMapping("/{attachmentId}/metadata")
    fun patchAttachmentMetadata(
        @AuthenticationPrincipal user: User,
        @PathVariable attachmentId: Long,
        @RequestBody request: PatchFileMetadataRequest
    ) = attachmentsService.patchAttachmentMetadata(user, attachmentId, request.filename)
}