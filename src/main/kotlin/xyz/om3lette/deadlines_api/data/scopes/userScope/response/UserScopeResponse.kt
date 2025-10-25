package xyz.om3lette.deadlines_api.data.scopes.userScope.response

import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeRole
import xyz.om3lette.deadlines_api.data.user.response.UserResponse
import java.time.Instant

data class UserScopeResponse(
    val user: UserResponse,

    val role: ScopeRole,

    val assignedAt: Instant
)