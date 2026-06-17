package xyz.om3lette.deadlines_api.data.scopes.deadline.dto

import xyz.om3lette.deadlines_api.data.scopes.deadline.response.DeadlineStatsResponse

data class DeadlineStatsDTO(
    val deadlineId: Long,
    val assignees: Long,
    val attachments: Long
) {
    fun toResponse(): DeadlineStatsResponse = DeadlineStatsResponse(
        assignees, attachments
    )
}
