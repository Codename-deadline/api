package xyz.om3lette.deadlines_api.data.scopes.deadline.response

import com.fasterxml.jackson.annotation.JsonUnwrapped
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeRole

data class DeadlineResponseWithRole(
    @get:JsonUnwrapped
    val deadlineResponse: DeadlineResponse,
    val role: ScopeRole?,
    val globalRole: ScopeRole?
)

