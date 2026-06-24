package xyz.om3lette.deadlines_api.data.permissions.dto

import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeRole

data class PermissionRoleDTO(
    val userId: Long,
    val role: ScopeRole
)
