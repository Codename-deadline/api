package xyz.om3lette.deadlines_api.data.attachments.request

import jakarta.validation.constraints.Size
import xyz.om3lette.deadlines_api.data.attachments.constraints.AttachmentConstraints
import xyz.om3lette.deadlines_api.data.common.validation.KnownPattern
import xyz.om3lette.deadlines_api.data.common.validation.enums.KnownPatternReason

data class PatchFileMetadataRequest(
    @field:KnownPattern(regexp = "(?s).*\\S.*", reason = KnownPatternReason.NOT_BLANK)
    @field:Size(min = AttachmentConstraints.FILENAME_MIN, max = AttachmentConstraints.FILENAME_MAX)
    val filename: String?
)
