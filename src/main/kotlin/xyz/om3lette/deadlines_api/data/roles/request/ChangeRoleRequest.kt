package xyz.om3lette.deadlines_api.data.roles.request

import jakarta.validation.constraints.NotBlank
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeRole

data class ChangeRoleRequest(
    @field:NotBlank
    val subjectUsername: String,

    val newRole: ScopeRole
)