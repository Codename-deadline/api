package xyz.om3lette.deadlines_api.services

import org.springframework.stereotype.Service
import xyz.om3lette.deadlines_api.data.permissions.dto.DeadlineScope
import xyz.om3lette.deadlines_api.data.permissions.dto.OrganizationScope
import xyz.om3lette.deadlines_api.data.permissions.dto.ThreadScope
import xyz.om3lette.deadlines_api.data.scopes.deadline.repo.DeadlineRepository
import xyz.om3lette.deadlines_api.data.scopes.thread.repo.ThreadRepository
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeRole
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeType
import xyz.om3lette.deadlines_api.data.scopes.userScope.repo.UserScopeRepository
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.exceptions.enums.ErrorCode
import xyz.om3lette.deadlines_api.exceptions.type.StatusCodeException
import xyz.om3lette.deadlines_api.services.permission.PermissionService
import xyz.om3lette.deadlines_api.util.jpaRepository.findByIdOr404
import xyz.om3lette.deadlines_api.util.requirePermission

@Service
class RolesService(
    private val userScopeRepository: UserScopeRepository,
    private val threadRepository: ThreadRepository,
    private val deadlineRepository: DeadlineRepository,
    private val permissionService: PermissionService
) {
    private fun filterRolesByPrefix(scopeRolePrefix: String): List<ScopeRole> =
        ScopeRole.entries.filter { role -> role.name.startsWith(scopeRolePrefix) }

    fun changeRole(
        issuer: User,
        scopeId: Long,
        subjectUsername: String,
        newRole: ScopeRole,
        scopeType: ScopeType
    ) {
        if (issuer.username.equals(subjectUsername, ignoreCase = true)) {
            throw StatusCodeException(400, ErrorCode.ROLE_CHANGE_SELF)
        }
        if (newRole !in filterRolesByPrefix(scopeType.code)) {
            throw StatusCodeException(400, ErrorCode.ROLE_CHANGE_INVALID_SCOPE_ROLE)
        }

        val permissionScope = when (scopeType) {
            ScopeType.ORGANIZATION -> OrganizationScope(scopeId)
            ScopeType.THREAD -> ThreadScope(threadRepository.findByIdOr404(scopeId, ErrorCode.THR_NOT_FOUND))
            ScopeType.DEADLINE -> DeadlineScope(deadlineRepository.findByIdOr404(scopeId, ErrorCode.DDL_NOT_FOUND))
        }
        requirePermission(
            permissionService.canChangeRole(issuer, permissionScope, newRole)
        )

        val currentSubjectScope = userScopeRepository.findByScopeTypeAndScopeIdAndUsernameIgnoreCase(
            subjectUsername, scopeType, scopeId
        ).orElseThrow {
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
