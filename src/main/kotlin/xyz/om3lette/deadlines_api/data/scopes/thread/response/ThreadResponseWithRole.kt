package xyz.om3lette.deadlines_api.data.scopes.thread.response

import com.fasterxml.jackson.annotation.JsonUnwrapped
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeRole

data class ThreadResponseWithRole(
    @get:JsonUnwrapped
    val threadResponse: ThreadResponse,
    val role: ScopeRole?,
    val globalRole: ScopeRole?
)
