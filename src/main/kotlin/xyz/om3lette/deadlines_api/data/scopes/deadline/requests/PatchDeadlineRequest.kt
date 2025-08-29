package xyz.om3lette.deadlines_api.data.scopes.deadline.requests

import xyz.om3lette.deadlines_api.data.scopes.enums.ProgressionStatus
import java.time.Instant

data class PatchDeadlineRequest(
    val title: String?,
    val description: String?,
    val status: ProgressionStatus?,
    val progress: Short?,
    val due: Instant?,
)
