package xyz.om3lette.deadlines_api.util.userScopeRepository

import xyz.om3lette.deadlines_api.data.scopes.userScope.repo.UserScopeRepository
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.exceptions.enums.ErrorCode
import xyz.om3lette.deadlines_api.exceptions.type.StatusCodeException

fun UserScopeRepository.findByUserAndScopeIdOr403(user: User, scopeId: Long) =
    findByUserAndScopeId(user, scopeId).orElseThrow {
        StatusCodeException(403, ErrorCode.AUTH_INSUFFICIENT_PERMISSIONS)
    }