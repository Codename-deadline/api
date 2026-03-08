package xyz.om3lette.deadlines_api.services.permission

import org.springframework.stereotype.Component
import org.springframework.web.context.annotation.RequestScope
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeRole

@Component
@RequestScope
class PermissionContext {

    private val cache = mutableMapOf<String, ScopeRole?>()

    fun getOrLoad(key: String, loader: () -> ScopeRole?): ScopeRole? {
        return cache.getOrPut(key) { loader() }
    }
}
