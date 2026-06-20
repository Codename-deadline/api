package xyz.om3lette.deadlines_api.data.scopes.deadline.requests

import java.time.Instant

data class PatchDeadlineRequest(
    val title: String?,
    val description: String?,
    val isCompleted: Boolean?,
    val due: Instant?,
)
