package xyz.om3lette.deadlines_api.data.scopes.common.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeRole

data class UsernameRolePair(
    @field:NotBlank val username: String,
    @field:NotNull val role: ScopeRole
)
