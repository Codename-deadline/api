package xyz.om3lette.deadlines_api.data.scopes.deadline.response

import xyz.om3lette.deadlines_api.data.scopes.enums.ProgressionStatus
import java.time.Instant

data class DeadlineResponse(
    val id: Long,

    val title: String,

    val description: String?,

    val createdAt: Instant,

    val due: Instant,

    val progress: Short,

    val status: ProgressionStatus
)