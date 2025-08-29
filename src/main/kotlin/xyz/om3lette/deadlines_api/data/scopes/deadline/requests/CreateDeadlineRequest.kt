package xyz.om3lette.deadlines_api.data.scopes.deadline.requests

import xyz.om3lette.deadlines_api.data.scopes.enums.ProgressionStatus
import java.time.Instant

data class CreateDeadlineRequest(
    val title: String,
    val description: String?,
    val status: ProgressionStatus,
    val due: Instant,
    val usernamesToAssign: List<String>
)
