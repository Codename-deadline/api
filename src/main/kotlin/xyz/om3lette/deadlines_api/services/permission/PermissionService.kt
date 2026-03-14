package xyz.om3lette.deadlines_api.services.permission

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import xyz.om3lette.deadlines_api.data.permissions.dto.DeadlineScope
import xyz.om3lette.deadlines_api.data.permissions.dto.OrganizationScope
import xyz.om3lette.deadlines_api.data.permissions.dto.PermissionScope
import xyz.om3lette.deadlines_api.data.permissions.dto.ThreadScope
import xyz.om3lette.deadlines_api.data.scopes.deadline.model.Deadline
import xyz.om3lette.deadlines_api.data.scopes.organization.dto.OrganizationPermissions
import xyz.om3lette.deadlines_api.data.scopes.organization.enums.OrganizationType
import xyz.om3lette.deadlines_api.data.scopes.organization.model.Organization
import xyz.om3lette.deadlines_api.data.scopes.thread.model.Thread
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeRole
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeType
import xyz.om3lette.deadlines_api.data.scopes.userScope.repo.UserScopeRepository
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.util.user.isAdminOr
import xyz.om3lette.deadlines_api.util.user.isAdminOrHasRoleAnd

@Service
class PermissionService(
    private val userScopeRepository: UserScopeRepository,
    private val permissionContext: PermissionContext
) {
    @Value("\${users.max-linked-accounts-per-messenger}")
    private var maxLinkedAccountsPerMessenger: Int = 5

    private val logger = LoggerFactory.getLogger(PermissionService::class.java)

    private fun roleForOrganizationLazy(user: User, organizationId: Long): () -> ScopeRole? =
        {
            permissionContext.getOrLoadBatch(OrganizationScope(organizationId)) {
                userScopeRepository.findUserRolesInScope(
                    userId = user.id, orgId = organizationId, null, null
                )
            }
        }

        private fun roleForThreadLazy(user: User, thread: Thread): () -> ScopeRole? =
        {
            permissionContext.getOrLoadBatch(ThreadScope(thread)) {
                userScopeRepository.findUserRolesInScope(
                    userId = user.id, orgId = thread.organization.id, thread.id, null
                )
            }
        }

    private fun roleForDeadlineLazy(user: User, deadline: Deadline): () -> ScopeRole? =
        {
            permissionContext.getOrLoadBatch(DeadlineScope(deadline)) {
                userScopeRepository.findUserRolesInScope(
                    userId = user.id, orgId = deadline.organization.id, deadline.thread.id, deadline.id
                )
            }
        }

    fun findRoleByPermissionScopeLazy(issuer: User, permissionScope: PermissionScope): () -> ScopeRole? =
        when (permissionScope) {
            is OrganizationScope -> roleForOrganizationLazy(issuer, permissionScope.orgId)
            is ThreadScope -> roleForThreadLazy(issuer, permissionScope.thread)
            is DeadlineScope -> roleForDeadlineLazy(issuer, permissionScope.deadline)
        }

    /*
        Organization permissions:
     */
    private fun hasOrganizationAccess(issuer: User, organization: Organization): Boolean =
        issuer.isAdminOr {
            if (organization.type == OrganizationType.PUBLIC) return true
            return roleForOrganizationLazy(issuer, organization.id)() != null
        }

    private fun canDeleteOrganization(issuer: User, organizationId: Long): Boolean =
        issuer.isAdminOrHasRoleAnd(roleForOrganizationLazy(issuer, organizationId)) { role ->
            role >= ScopeRole.ORG_OWNER
        }

    private fun canUpdateOrganization(issuer: User, organizationId: Long): Boolean =
        issuer.isAdminOrHasRoleAnd(roleForOrganizationLazy(issuer, organizationId)) { role ->
            role >= ScopeRole.ORG_OWNER
        }

    private fun canManageOrganizationMembers(issuer: User, organizationId: Long): Boolean =
        issuer.isAdminOrHasRoleAnd(roleForOrganizationLazy(issuer, organizationId)) { role ->
            role >= ScopeRole.ORG_ADMIN
        }

    fun prefetchUserRoles(
        user: User,
        orgIds: List<Long> = emptyList(),
        thrIds: List<Long> = emptyList(),
        ddlIds: List<Long> = emptyList()
    ) {
        if (orgIds.isEmpty() && thrIds.isEmpty() && ddlIds.isNotEmpty()) return
        permissionContext.putAll(
            userScopeRepository.findUserRolesInScopes(
                user.id, orgIds, thrIds, ddlIds
            )
        )
    }


    /**
     * Computes all relevant organization permissions/
     *
     * IMPORTANT: if calling for a list of unique organization it is advices to call `prefetchUserRoles` first
     */
    fun buildOrganizationPermissions(issuer: User, organizationId: Long) = OrganizationPermissions(
        update = canUpdateOrganization(issuer, organizationId),
        delete = canDeleteOrganization(issuer, organizationId),
        manageRoles = canManageOrganizationMembers(issuer, organizationId),
        invite = canSendOrganizationInvitation(issuer, organizationId),
    )

    /*
        Thread permissions:
     */
    private fun hasThreadAccess(issuer: User, thread: Thread): Boolean {
        if (thread.organization.type == OrganizationType.PUBLIC) return true
        return issuer.isAdminOrHasRoleAnd(roleForThreadLazy(issuer, thread)) { role ->
            role >= ScopeRole.THR_ASSIGNEE
        }
    }

    fun canCreateThread(issuer: User, organizationId: Long): Boolean =
        issuer.isAdminOrHasRoleAnd(roleForOrganizationLazy(issuer, organizationId)) { role ->
            role >= ScopeRole.ORG_ADMIN
        }

    private fun canDeleteThread(issuer: User, thread: Thread): Boolean =
        issuer.isAdminOrHasRoleAnd(roleForThreadLazy(issuer, thread)) { role ->
            role >= ScopeRole.ORG_ADMIN
        }

    private fun canUpdateThread(issuer: User, thread: Thread): Boolean =
        issuer.isAdminOrHasRoleAnd(roleForThreadLazy(issuer, thread)) { role ->
            role >= ScopeRole.ORG_ADMIN
        }

    private fun canManageThreadAssignees(issuer: User, thread: Thread): Boolean =
        issuer.isAdminOrHasRoleAnd(roleForThreadLazy(issuer, thread)) { role ->
            role >= ScopeRole.ORG_ADMIN
        }

    /*
        Deadline permissions:
     */
    private fun hasDeadlineAccess(issuer: User, deadline: Deadline): Boolean {
        if (deadline.organization.type == OrganizationType.PUBLIC) return true
        return issuer.isAdminOrHasRoleAnd(roleForDeadlineLazy(issuer, deadline)) { role ->
            role >= ScopeRole.DDL_ASSIGNEE
        }
    }

    fun canCreateDeadline(issuer: User, thread: Thread): Boolean =
        issuer.isAdminOrHasRoleAnd(roleForThreadLazy(issuer, thread)) { role ->
            role >= ScopeRole.THR_ASSIGNEE
        }

    private fun canDeleteDeadline(issuer: User, deadline: Deadline): Boolean =
        issuer.isAdminOrHasRoleAnd(roleForDeadlineLazy(issuer, deadline)) { role ->
            role >= ScopeRole.THR_ASSIGNEE
        }

    private fun canUpdateDeadline(issuer: User, deadline: Deadline): Boolean =
        issuer.isAdminOrHasRoleAnd(roleForDeadlineLazy(issuer, deadline)) { role ->
            role >= ScopeRole.THR_ASSIGNEE
        }

    private fun canManageDeadlineAssignees(issuer: User, deadline: Deadline): Boolean =
        issuer.isAdminOrHasRoleAnd(roleForDeadlineLazy(issuer, deadline)) { role ->
            role >= ScopeRole.THR_ASSIGNEE
        }

    fun canManageDeadlineAttachments(issuer: User, deadline: Deadline): Boolean =
        issuer.isAdminOrHasRoleAnd(roleForDeadlineLazy(issuer, deadline)) { role ->
            role >= ScopeRole.DDL_ASSIGNEE
        }

    /*
        Helper wrappers
     */
    fun canManageAssignees(issuer: User, permissionScope: PermissionScope) =
        when (permissionScope) {
            is OrganizationScope -> canManageOrganizationMembers(issuer, permissionScope.orgId)
            is ThreadScope -> canManageThreadAssignees(issuer, permissionScope.thread)
            is DeadlineScope -> canManageDeadlineAssignees(issuer, permissionScope.deadline)
        }

    /**
     * Checks if a user has access to a scope.
     *
     * **WARNING**: `OrganizationScope.organization` must be provided
     */
    fun hasAccess(issuer: User, permissionScope: PermissionScope) =
        when (permissionScope) {
            is OrganizationScope ->
                if (permissionScope.organization != null) {
                    hasOrganizationAccess(issuer, permissionScope.organization)
                } else {
                    logger.error("Organization field was not provided. Unable to verify organization access")
                    false
                }
            is ThreadScope -> hasThreadAccess(issuer, permissionScope.thread)
            is DeadlineScope -> hasDeadlineAccess(issuer, permissionScope.deadline)
        }

    fun canDelete(issuer: User, permissionScope: PermissionScope) =
        when (permissionScope) {
            is OrganizationScope -> canDeleteOrganization(issuer, permissionScope.orgId)
            is ThreadScope -> canDeleteThread(issuer, permissionScope.thread)
            is DeadlineScope -> canDeleteDeadline(issuer, permissionScope.deadline)
        }

    fun canUpdate(issuer: User, permissionScope: PermissionScope) =
        when (permissionScope) {
            is OrganizationScope -> canUpdateOrganization(issuer, permissionScope.orgId)
            is ThreadScope -> canUpdateThread(issuer, permissionScope.thread)
            is DeadlineScope -> canUpdateDeadline(issuer, permissionScope.deadline)
        }

    /*
        Invitation permissions:
    */
    fun canSendOrganizationInvitation(issuer: User, organizationId: Long): Boolean =
        issuer.isAdminOrHasRoleAnd(roleForOrganizationLazy(issuer, organizationId)) { role ->
            role >= ScopeRole.ORG_ADMIN
        }

    /*
        Integration permissions
     */
    fun canLinkAccount(issuer: User, accountsLinkedForMessenger: Int) =
        issuer.isAdminOr {
            accountsLinkedForMessenger < maxLinkedAccountsPerMessenger
        }
    
    fun canRegisterChat(user: User?) = user != null

    /*
        Roles permissions
     */

    /**
     * Checks if issuer's role `issuerCurrentRole` is high enough to assign `roleToAssign`.
     */
    private fun canAssignWithCurrentRole(issuerCurrentRole: ScopeRole, roleToAssign: ScopeRole): Boolean {
        return roleToAssign < issuerCurrentRole
    }

    fun canChangeRole(issuer: User, permissionScope: PermissionScope, newRole: ScopeRole): Boolean {
            if (!canManageAssignees(issuer, permissionScope)) return false
            return issuer.isAdminOrHasRoleAnd(findRoleByPermissionScopeLazy(issuer, permissionScope)) { role ->
                // '<'  Implicitly forbids multiple organization owners
                canAssignWithCurrentRole(role, newRole)
            }
        }

    /**
     * Calculates whether a user with role `role` can assign each role in `scopeRoles` presuming that he
     * has canManage permission for the scope
     */
    fun canReassignWithTheGivenRole(role: ScopeRole, scopeRoles: List<ScopeRole>): List<Boolean> {
        val canAssign: MutableList<Boolean> = mutableListOf()
        for (attemptedRole in scopeRoles) {
            canAssign.add(canAssignWithCurrentRole(role, attemptedRole))
        }
        return canAssign.toList()
    }

    fun getRole(scopeId: Long, scopeType: ScopeType) = permissionContext.get(scopeId, scopeType)
}
