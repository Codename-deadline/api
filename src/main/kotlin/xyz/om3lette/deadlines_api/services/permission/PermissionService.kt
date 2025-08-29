package xyz.om3lette.deadlines_api.services.permission

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import xyz.om3lette.deadlines_api.data.scopes.organization.enums.OrganizationType
import xyz.om3lette.deadlines_api.data.scopes.organization.model.Organization
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeRole
import xyz.om3lette.deadlines_api.data.scopes.userScope.model.UserScope
import xyz.om3lette.deadlines_api.data.scopes.userScope.roleIsEqualOrHigherThan
import xyz.om3lette.deadlines_api.util.user.isAdminOr
import xyz.om3lette.deadlines_api.util.user.isAdminOrHasRoleAnd
import xyz.om3lette.deadlines_api.data.user.model.User
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

@Service
class PermissionService() {
    @Value("\${users.max-linked-accounts-per-messenger}")
    private var maxLinkedAccountsPerMessenger: Int = 5

    /*
        Organization permissions:
     */
    fun hasOrganizationAccess(issuer: User, organization: Organization, userScopeLazy: () -> Optional<UserScope>): Boolean =
        issuer.isAdminOr {
            if (organization.type == OrganizationType.PUBLIC) return true
            return userScopeLazy().isPresent
        }

    fun canDeleteOrganization(issuer: User, userScopeLazy: () -> Optional<UserScope>): Boolean =
        issuer.isAdminOrHasRoleAnd(userScopeLazy) { scope ->
            scope.roleIsEqualOrHigherThan(ScopeRole.ORG_OWNER)
        }

    fun canUpdateOrganization(issuer: User, userScopeLazy: () -> Optional<UserScope>): Boolean =
        issuer.isAdminOrHasRoleAnd(userScopeLazy) { scope ->
            scope.roleIsEqualOrHigherThan(ScopeRole.ORG_OWNER)
        }

    fun canManageOrganizationMembers(issuer: User, userScopeLazy: () -> Optional<UserScope>): Boolean =
        issuer.isAdminOrHasRoleAnd(userScopeLazy) { scope ->
            scope.roleIsEqualOrHigherThan(ScopeRole.ORG_ADMIN)
        }

    /*
        Thread permissions:
     */
    fun hasThreadAccess(issuer: User, userScopeLazy: () -> Optional<UserScope>, organization: Organization): Boolean =
        issuer.isAdminOr {
            if (organization.type == OrganizationType.PUBLIC) return true
            userScopeLazy().getOrNull()?.roleIsEqualOrHigherThan(ScopeRole.THR_ASSIGNEE) ?: false
        }

    fun hasThreadAccess(issuer: User, userScopeLazy: () -> Optional<UserScope>, organizationLazy: () -> Organization): Boolean =
        issuer.isAdminOr {
            if (organizationLazy().type == OrganizationType.PUBLIC) return true
            userScopeLazy().getOrNull()?.roleIsEqualOrHigherThan(ScopeRole.THR_ASSIGNEE) ?: false
        }

    fun canCreateOrDeleteThread(issuer: User, userScopeLazy: () -> Optional<UserScope>): Boolean =
        issuer.isAdminOrHasRoleAnd(userScopeLazy) { scope ->
            scope.roleIsEqualOrHigherThan(ScopeRole.ORG_ADMIN)
        }

    fun canUpdateThread(issuer: User, userScopeLazy: () -> Optional<UserScope>): Boolean =
        issuer.isAdminOrHasRoleAnd(userScopeLazy) { scope ->
            scope.roleIsEqualOrHigherThan(ScopeRole.ORG_ADMIN)
        }

    fun canManageThreadAssignees(issuer: User, userScopeLazy: () -> Optional<UserScope>): Boolean =
        issuer.isAdminOrHasRoleAnd(userScopeLazy) { scope ->
            scope.roleIsEqualOrHigherThan(ScopeRole.ORG_ADMIN)
        }

    /*
        Deadline permissions:
     */
    fun hasDeadlineAccess(issuer: User, issuerScopeLazy: () -> Optional<UserScope>, organization: Organization): Boolean =
        issuer.isAdminOr {
            if (organization.type == OrganizationType.PUBLIC) return true
            issuerScopeLazy().getOrNull()?.roleIsEqualOrHigherThan(ScopeRole.DDL_ASSIGNEE) ?: false
        }

    fun canCreateOrDeleteDeadline(issuer: User, userScopeLazy: () -> Optional<UserScope>): Boolean =
        issuer.isAdminOrHasRoleAnd(userScopeLazy) { scope ->
            scope.roleIsEqualOrHigherThan(ScopeRole.THR_ASSIGNEE)
        }

    fun canUpdateDeadline(issuer: User, userScopeLazy: () -> Optional<UserScope>): Boolean =
        issuer.isAdminOrHasRoleAnd(userScopeLazy) { scope ->
            scope.roleIsEqualOrHigherThan(ScopeRole.THR_ASSIGNEE)
        }

    fun canManageDeadlineAssignees(issuer: User, userScopeLazy: () -> Optional<UserScope>): Boolean =
        issuer.isAdminOrHasRoleAnd(userScopeLazy) { scope ->
            scope.roleIsEqualOrHigherThan(ScopeRole.THR_ASSIGNEE)
        }

    fun canManageDeadlineAttachments(issuer: User, userScopeLazy: () -> Optional<UserScope>): Boolean =
        issuer.isAdminOrHasRoleAnd(userScopeLazy) { scope ->
            scope.roleIsEqualOrHigherThan(ScopeRole.DDL_ASSIGNEE)
        }

    /*
        Invitation permissions:
    */
    fun canSendOrganizationInvitation(issuer: User, userScopeLazy: () -> Optional<UserScope>): Boolean =
        issuer.isAdminOrHasRoleAnd(userScopeLazy) { scope ->
            scope.roleIsEqualOrHigherThan(ScopeRole.ORG_ADMIN)
        }

    /*
        Integration permissions
     */
    fun canLinkAccount(user: User, accountsLinkedForMessenger: Int) =
        user.isAdminOr {
            accountsLinkedForMessenger < maxLinkedAccountsPerMessenger
        }
    
    fun canRegisterChat(user: User?) = user != null

    /*
        Roles permissions
     */
    fun canChangeRole(user: User,  newRole: ScopeRole, userScopeLazy: () -> Optional<UserScope>) =
        user.isAdminOr {
            (userScopeLazy().getOrNull()?.role ?: ScopeRole.ORG_MEMBER).isHigherThan(newRole)
        }
}