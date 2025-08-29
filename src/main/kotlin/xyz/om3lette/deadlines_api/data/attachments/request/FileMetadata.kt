package xyz.om3lette.deadlines_api.data.attachments.request

import jakarta.validation.constraints.Size

data class FileMetadata(
    @field:Size(min = 1, max = 64)
    val filename: String
)
