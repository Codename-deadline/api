package xyz.om3lette.deadlines_api.data.scopes.deadline.dto

import xyz.om3lette.deadlines_api.data.scopes.enums.ProgressionStatus
import java.time.Instant

data class DeadlineDTO(
    val id: Long,

    val title: String,

    val description: String?,

    val createdAt: Instant,

    val due: Instant,

    val status: ProgressionStatus,

    val organizationId: Long
)
