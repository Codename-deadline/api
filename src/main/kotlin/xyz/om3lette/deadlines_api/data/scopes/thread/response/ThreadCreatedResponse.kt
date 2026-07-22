package xyz.om3lette.deadlines_api.data.scopes.thread.response

import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeRole

data class ThreadCreatedResponse(
    val threadId: Long,
    val assignees: Int,
    val globalRole: ScopeRole
)
