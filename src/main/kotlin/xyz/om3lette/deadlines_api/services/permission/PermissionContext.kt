package xyz.om3lette.deadlines_api.services.permission

import org.springframework.stereotype.Component
import org.springframework.web.context.annotation.RequestScope
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeRole
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeType

@Component
@RequestScope
class PermissionContext {

    private val cache = mutableMapOf<String, ScopeRole?>()

    private fun buildPermissionKey(userId: Long, scopeType: ScopeType, scopeId: Long) =
        "${scopeType}:${userId}@${scopeId}"

    fun getOrLoad(userId: Long, scopeType: ScopeType, scopeId: Long, loader: () -> ScopeRole?): ScopeRole? {
        return cache.getOrPut(
            buildPermissionKey(userId, scopeType, scopeId)
        ) { loader() }
    }
}
