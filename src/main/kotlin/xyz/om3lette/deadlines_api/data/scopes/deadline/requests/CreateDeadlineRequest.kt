package xyz.om3lette.deadlines_api.data.scopes.deadline.requests

import xyz.om3lette.deadlines_api.data.scopes.common.dto.UsernameRolePairList
import java.time.Instant

data class CreateDeadlineRequest(
    val title: String,
    val description: String?,
    val due: Instant,
    val invitations: UsernameRolePairList
)
