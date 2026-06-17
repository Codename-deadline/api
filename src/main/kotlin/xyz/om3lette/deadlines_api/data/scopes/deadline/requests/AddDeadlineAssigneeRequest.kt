package xyz.om3lette.deadlines_api.data.scopes.deadline.requests

import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeRole

data class AddDeadlineAssigneeRequest(
    val username: String,
    val role: ScopeRole
)
