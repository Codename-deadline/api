package xyz.om3lette.deadlines_api.data.attachments.reponse

import xyz.om3lette.deadlines_api.data.user.response.MinimalUserResponse
import java.time.Instant

data class AttachmentResponse(
    val id: Long,

    val filename: String,

    val category: String,

    val mimeType: String,

    val sizeBytes: Long,

    val uploadedBy: MinimalUserResponse,

    val attachedTo: Long,

    val uploadedAt: Instant
)
