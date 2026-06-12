package xyz.om3lette.deadlines_api.data.scopes.thread.dto

import xyz.om3lette.deadlines_api.data.scopes.thread.response.ThreadStats

data class ThreadStatsDTO(
    val threadId: Long,
    val assignees: Long,
    val deadlines: Long,
    val completedDeadlines: Long
) {
    fun toResponse(): ThreadStats = ThreadStats(
        assignees, deadlines, completedDeadlines
    )
}
