package xyz.om3lette.deadlines_api.data.scopes.deadline.response

import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeRole

data class DeadlineCreatedResponse(
    val deadlineId: Long,
    val assignees: Int,
    val globalRole: ScopeRole,
)
