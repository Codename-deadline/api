package xyz.om3lette.deadlines_api.data.scopes.userScope.dto

import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeRole
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeType

data class ScopeRoleDTO(
    val role: ScopeRole,
    val scopeId: Long,
    val scopeType: ScopeType
)