package xyz.om3lette.deadlines_api.controllers.attachments

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Positive
import org.springframework.http.MediaType
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import xyz.om3lette.deadlines_api.data.attachments.request.FileMetadata
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.services.storage.AttachmentsService

@SecurityRequirement(name = "bearerAuth")
@Validated
@RestController
@RequestMapping("/deadlines/{deadlineId}/attachments")
@Tag(name = "Deadlines")
class DeadlineAttachmentController(
    private val attachmentsService: AttachmentsService
) {
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(summary = "Create a deadline attachment")
    fun createAttachment(
        @AuthenticationPrincipal user: User,
        @PathVariable @Positive deadlineId: Long,
        @RequestPart("meta") @Valid meta: FileMetadata,
        @RequestPart("file") file: MultipartFile
    ) = attachmentsService.createAttachment(user, deadlineId, file, meta.filename)

    @GetMapping
    @Operation(summary = "Get metadata of all deadline attachments")
    fun getDeadlineAttachmentsMetadata(
        @AuthenticationPrincipal user: User,
        @PathVariable @Positive deadlineId: Long
    ) = attachmentsService.getDeadlineAttachmentsMetadata(user, deadlineId)
}
