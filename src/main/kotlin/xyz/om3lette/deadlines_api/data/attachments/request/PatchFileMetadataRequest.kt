package xyz.om3lette.deadlines_api.data.attachments.request

import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import xyz.om3lette.deadlines_api.data.attachments.constraints.AttachmentConstraints

data class PatchFileMetadataRequest(
    @field:Pattern(regexp = ".*\\S.*")
    @field:Size(min = AttachmentConstraints.FILENAME_MIN, max = AttachmentConstraints.FILENAME_MAX)
    val filename: String?
)
