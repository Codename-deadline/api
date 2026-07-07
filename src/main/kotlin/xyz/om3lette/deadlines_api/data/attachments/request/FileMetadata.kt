package xyz.om3lette.deadlines_api.data.attachments.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import xyz.om3lette.deadlines_api.data.attachments.constraints.AttachmentConstraints

data class FileMetadata(
    @field:NotBlank
    @field:Size(min = AttachmentConstraints.FILENAME_MIN, max = AttachmentConstraints.FILENAME_MAX)
    val filename: String
)
