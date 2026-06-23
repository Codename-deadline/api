package xyz.om3lette.deadlines_api.data.scopes.deadline.response

import com.fasterxml.jackson.annotation.JsonUnwrapped
import xyz.om3lette.deadlines_api.data.scopes.deadline.dto.DeadlineDTO
import xyz.om3lette.deadlines_api.data.scopes.deadline.dto.DeadlinePermissions
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeRole

data class DeadlineResponse(
    @get:JsonUnwrapped
    val deadline: DeadlineDTO,
    val stats: DeadlineStatsResponse,
    val permissions: DeadlinePermissions
) {
    fun withRole(scopeRole: ScopeRole?, globalRole: ScopeRole?): DeadlineResponseWithRole = DeadlineResponseWithRole(
        this, role = scopeRole, globalRole = globalRole
    )
}
