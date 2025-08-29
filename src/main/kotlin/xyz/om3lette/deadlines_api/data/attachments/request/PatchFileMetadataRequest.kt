package xyz.om3lette.deadlines_api.data.attachments.request

import jakarta.validation.constraints.Size

data class PatchFileMetadataRequest(
    @field:Size(min = 1, max = 64)
    val filename: String?
)