package xyz.om3lette.deadlines_api.services

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import xyz.om3lette.deadlines_api.data.permissions.dto.DeadlineScope
import xyz.om3lette.deadlines_api.data.permissions.dto.OrganizationScope
import xyz.om3lette.deadlines_api.data.permissions.dto.PermissionRoleDTO
import xyz.om3lette.deadlines_api.data.permissions.dto.ThreadScope
import xyz.om3lette.deadlines_api.data.roles.response.RolesMetadataResponse
import xyz.om3lette.deadlines_api.data.scopes.deadline.repo.DeadlineRepository
import xyz.om3lette.deadlines_api.data.scopes.organization.repo.OrganizationRepository
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
    private val organizationRepository: OrganizationRepository,
    private val threadRepository: ThreadRepository,
    private val deadlineRepository: DeadlineRepository,
    private val permissionService: PermissionService
) {
    val metadata: RolesMetadataResponse by lazy {
        val roles: List<String> = ScopeRole.entries.map { it.name }
        val matrix: List<List<Boolean>> = ScopeRole.entries.map {
            permissionService.canReassignWithTheGivenRole(it, ScopeRole.entries)
        }

        RolesMetadataResponse(roles, matrix)
    }

    @Transactional
    fun changeRole(
        issuer: User,
        scopeId: Long,
        subjectUsername: String,
        newRole: ScopeRole,
        scopeType: ScopeType
    ) {
        if (newRole == ScopeRole.ORG_OWNER) {
            throw StatusCodeException(400, ErrorCode.ROLE_IMPLICIT_OWNERSHIP_CHANGE)
        }
        if (issuer.username.equals(subjectUsername, ignoreCase = true)) {
            throw StatusCodeException(400, ErrorCode.ROLE_CHANGE_SELF)
        }
        if (!newRole.canBeAssignedInScope(scopeType)) {
            throw StatusCodeException(400, ErrorCode.ROLE_CHANGE_INVALID_SCOPE_ROLE)
        }

        val permissionScope = when (scopeType) {
            ScopeType.ORGANIZATION -> {
                organizationRepository.findByIdForUpdate(scopeId)
                    ?: throw StatusCodeException(404, ErrorCode.ORG_NOT_FOUND)
                OrganizationScope(scopeId)
            }
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
        if (currentSubjectScope.role == ScopeRole.ORG_OWNER) {
            throw StatusCodeException(400, ErrorCode.ROLE_IMPLICIT_OWNERSHIP_CHANGE)
        }
        if (currentSubjectScope.role != newRole) {
            currentSubjectScope.role = newRole
            userScopeRepository.save(currentSubjectScope)
        }
    }

    @Transactional
    fun changeOrganizationOwner(
        issuer: User,
        organizationId: Long,
        newOwnerUsername: String
    ) {
        if (issuer.username.equals(newOwnerUsername, ignoreCase = true)) {
            throw StatusCodeException(400, ErrorCode.ROLE_CHANGE_SELF)
        }
        organizationRepository.findByIdForUpdate(organizationId)
            ?: throw StatusCodeException(404, ErrorCode.ORG_NOT_FOUND)

        val currentRoles = userScopeRepository.findOrganizationRolesForOwnerTransfer(
            ownerId = issuer.id,
            newOwnerUsernameLower = newOwnerUsername.lowercase(),
            organizationId = organizationId
        )

        val issuerRole = currentRoles.find { it.userId == issuer.id }?.role
        if (issuerRole != ScopeRole.ORG_OWNER) {
            throw StatusCodeException(403, ErrorCode.AUTH_INSUFFICIENT_PERMISSIONS)
        }

        val allegedNewOwnerRole: PermissionRoleDTO = currentRoles.find { it.userId != issuer.id }
            ?: throw StatusCodeException(404, ErrorCode.MEMBER_NOT_FOUND)

        val demotedOwners = userScopeRepository.updateRoleByUserIdAndScopeIdAndScopeType(
            issuer.id, ScopeRole.ORG_ADMIN, organizationId, ScopeType.ORGANIZATION
        )
        if (demotedOwners != 1) {
            throw StatusCodeException(500, ErrorCode.UNKNOWN_ERROR, "Expected exactly one current organization owner")
        }

        val promotedOwners = userScopeRepository.updateRoleByUserIdAndScopeIdAndScopeType(
            allegedNewOwnerRole.userId,
            ScopeRole.ORG_OWNER,
            organizationId,
            ScopeType.ORGANIZATION
        )
        if (promotedOwners != 1) {
            throw StatusCodeException(500, ErrorCode.UNKNOWN_ERROR, "Expected exactly one new organization owner")
        }
    }
}
