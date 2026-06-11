package xyz.om3lette.deadlines_api.data.scopes.thread.response

import com.fasterxml.jackson.annotation.JsonUnwrapped
import xyz.om3lette.deadlines_api.data.scopes.thread.dto.ThreadDTO
import xyz.om3lette.deadlines_api.data.scopes.thread.dto.ThreadPermissions
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeRole

data class ThreadResponse(
    @get:JsonUnwrapped
    val thread: ThreadDTO,
    val stats: ThreadStats,
    val permissions: ThreadPermissions
) {
    fun withRole(scopeRole: ScopeRole?, globalRole: ScopeRole?): ThreadResponseWithRole = ThreadResponseWithRole(
        this, role = scopeRole, globalRole = globalRole
    )
}
