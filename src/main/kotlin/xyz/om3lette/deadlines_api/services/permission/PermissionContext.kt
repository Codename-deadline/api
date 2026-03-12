package xyz.om3lette.deadlines_api.services.permission

import org.springframework.stereotype.Component
import org.springframework.web.context.annotation.RequestScope
import xyz.om3lette.deadlines_api.data.permissions.dto.DeadlineScope
import xyz.om3lette.deadlines_api.data.permissions.dto.OrganizationScope
import xyz.om3lette.deadlines_api.data.permissions.dto.PermissionScope
import xyz.om3lette.deadlines_api.data.permissions.dto.ThreadScope
import xyz.om3lette.deadlines_api.data.scopes.userScope.dto.ScopeRoleDTO
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeRole
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeType

@Component
@RequestScope
class PermissionContext {
    data class PermissionKey(
        val scopeType: ScopeType,
        val scopeId: Long
    )

    private val cache = mutableMapOf<PermissionKey, ScopeRole?>()

    fun get(scopeId: Long, scopeType: ScopeType): ScopeRole? =
        cache[PermissionKey(scopeType, scopeId)]

    /**
     * Returns the highest role for a user in a given scope.
     *
     * For organization: organization role
     * For thread: max(organization, thread)
     * For deadline: max(organization, thread, deadline)
     *
     * If either of the roles is missing in the cache null will be returned.
     */
    fun getHighestRole(permissionScope: PermissionScope): ScopeRole? =
        when (permissionScope) {
            is OrganizationScope -> get(permissionScope.orgId, ScopeType.ORGANIZATION)
            is ThreadScope -> {
                val orgRole = getHighestRole(
                    OrganizationScope(permissionScope.thread.organization.id)
                ) ?: return null
                val threadRole = get(permissionScope.thread.id, ScopeType.THREAD) ?: return null
                return maxOf(orgRole, threadRole)
            }
            is DeadlineScope -> {
                val maxOfThreadAndOrgRole = getHighestRole(
                    ThreadScope(permissionScope.deadline.thread)
                ) ?: return null
                val deadlineRole = get(permissionScope.deadline.id, ScopeType.DEADLINE) ?: return null
                return maxOf(maxOfThreadAndOrgRole, deadlineRole)
            }
        }

    fun put(roleDTO: ScopeRoleDTO) =
        cache.put(PermissionKey(roleDTO.scopeType, roleDTO.scopeId), roleDTO.role)

    fun putAll(roles: List<ScopeRoleDTO>) {
        for (role in roles) {
            cache[PermissionKey(role.scopeType, role.scopeId)] = role.role
        }
    }

    fun getOrLoadBatch(permissionScope: PermissionScope, loader: () -> List<ScopeRoleDTO>): ScopeRole? {
        val role = getHighestRole(permissionScope)
        if (role != null) return role
        putAll(loader())
        return getHighestRole(permissionScope)
    }
}
