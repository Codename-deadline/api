package xyz.om3lette.deadlines_api.util.user

import xyz.om3lette.deadlines_api.data.user.enums.UserRole
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.data.scopes.userScope.model.UserScope
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

inline fun User.isAdminOr(then: () -> Boolean): Boolean {
    if (role == UserRole.ADMIN) return true
    return then()
}

inline fun User.isAdminOrHasRoleAnd(userScope: Optional<UserScope>, then: (userScope: UserScope) -> Boolean): Boolean =
    isAdminOr {
        val scope: UserScope = userScope.getOrNull() ?: return false
        return then(scope)
    }


inline fun User.isAdminOrHasRoleAnd(userScopeLazy: () -> Optional<UserScope>, then: (userScope: UserScope) -> Boolean): Boolean =
    isAdminOr {
        val scope: UserScope = userScopeLazy().getOrNull() ?: return false
        return then(scope)
    }
