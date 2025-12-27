package xyz.om3lette.deadlines_api.services

import org.springframework.stereotype.Service
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeRole
import xyz.om3lette.deadlines_api.data.scopes.userScope.repo.UserScopeRepository
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.exceptions.enums.ErrorCode
import xyz.om3lette.deadlines_api.exceptions.type.StatusCodeException
import xyz.om3lette.deadlines_api.services.permission.PermissionService
import xyz.om3lette.deadlines_api.util.requirePermission

@Service
class RolesService(
    private val userScopeRepository: UserScopeRepository,
    private val permissionService: PermissionService
) {
    private fun filterRolesByPrefix(scopeRolePrefix: String): List<ScopeRole> =
        ScopeRole.entries.filter { role -> role.name.startsWith(scopeRolePrefix) }

    fun changeRole(
        issuer: User,
        scopeId: Long,
        subjectUsername: String,
        newRole: ScopeRole,
        scopeRolePrefix: String
    ) {
        if (issuer.username.equals(subjectUsername, ignoreCase = true)) {
            throw StatusCodeException(400, ErrorCode.ROLE_CHANGE_SELF)
        }
        if (newRole !in filterRolesByPrefix(scopeRolePrefix)) {
            throw StatusCodeException(400, ErrorCode.ROLE_CHANGE_INVALID_SCOPE_ROLE)
        }
        val issuerScope = userScopeRepository.findByUserAndScopeId(issuer, scopeId)
        requirePermission(
            permissionService.canChangeRole(issuer, newRole) { issuerScope }
        )

        val currentSubjectScope = userScopeRepository.findByUsernameAndScopeIdIgnoreCase(subjectUsername, scopeId).orElseThrow {
            StatusCodeException(
                statusCode = 400,
                code = ErrorCode.ROLE_CHANGE_NO_ROLE,
                detail = "Subject does not have a role. Assign one first"
            )
        }
        if (currentSubjectScope.role != newRole) {
            currentSubjectScope.role = newRole
            userScopeRepository.save(currentSubjectScope)
        }
    }
}
