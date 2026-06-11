package xyz.om3lette.deadlines_api.data.scopes.thread.request

import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeRole

data class AddThreadAssigneeRequest(
    val username: String,
    val role: ScopeRole
)
