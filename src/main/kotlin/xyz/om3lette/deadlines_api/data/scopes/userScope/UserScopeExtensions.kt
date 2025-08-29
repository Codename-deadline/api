package xyz.om3lette.deadlines_api.data.scopes.userScope

import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeRole
import xyz.om3lette.deadlines_api.data.scopes.userScope.model.UserScope

fun UserScope.roleIsEqualOrHigherThan(minimalRole: ScopeRole) = role.isEqualOrHigherThan(minimalRole)
