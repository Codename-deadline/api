package xyz.om3lette.deadlines_api.data.attachments.reponse

import xyz.om3lette.deadlines_api.data.user.response.UserResponse
import java.time.Instant

data class AttachmentResponse(
    val id: Long,

    val filename: String,

    val category: String,

    val mimeType: String,

    val sizeBytes: Long,

    val uploadedBy: UserResponse,

    val attachedTo: Long,

    val uploadedAt: Instant
)
