package xyz.om3lette.deadlines_api.util.user

import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeRole
import xyz.om3lette.deadlines_api.data.user.enums.UserRole
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.data.scopes.userScope.model.UserScope
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

inline fun User.isAdminOr(then: () -> Boolean): Boolean {
    if (role == UserRole.ADMIN) return true
    return then()
}

inline fun User.isAdminOrHasRoleAnd(roleLazy: () -> ScopeRole?, then: (role: ScopeRole) -> Boolean): Boolean =
    isAdminOr {
        val scope: ScopeRole = roleLazy() ?: return false
        return then(scope)
    }
