package xyz.om3lette.deadlines_api.data.scopes.common.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeRole
import xyz.om3lette.deadlines_api.data.user.constraints.UserConstraints

data class UsernameRolePair(
    @field:NotBlank
    @field:Size(min = UserConstraints.USERNAME_MIN, max = UserConstraints.USERNAME_MAX)
    val username: String,

    @field:NotNull val role: ScopeRole
)
