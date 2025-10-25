package xyz.om3lette.deadlines_api.services

import org.springframework.stereotype.Service
import xyz.om3lette.deadlines_api.data.scopes.organization.enums.InvitationStatus
import xyz.om3lette.deadlines_api.data.scopes.organization.enums.OrganizationType
import xyz.om3lette.deadlines_api.data.scopes.organization.model.Organization
import xyz.om3lette.deadlines_api.data.scopes.organization.model.OrganizationInvitation
import xyz.om3lette.deadlines_api.data.scopes.organization.repo.OrganizationInvitationRepository
import xyz.om3lette.deadlines_api.data.scopes.organization.repo.OrganizationRepository
import xyz.om3lette.deadlines_api.data.scopes.organization.response.member.InvitationCreatedResponse
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.data.user.repo.UserRepository
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeRole
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeType
import xyz.om3lette.deadlines_api.data.scopes.userScope.model.UserScope
import xyz.om3lette.deadlines_api.data.scopes.userScope.repo.UserScopeRepository
import xyz.om3lette.deadlines_api.exceptions.type.StatusCodeException
import xyz.om3lette.deadlines_api.services.permission.PermissionService
import xyz.om3lette.deadlines_api.util.jpaRepository.findByIdOr404
import xyz.om3lette.deadlines_api.util.requirePermission
import xyz.om3lette.deadlines_api.util.userRepository.findByUsernameIgnoreCaseOr404
import java.time.Instant

@Service
class OrganizationInvitationService(
    private val userRepository: UserRepository,
    private val userScopeRepository: UserScopeRepository,
    private val organizationRepository: OrganizationRepository,
    private val organizationInvitationRepository: OrganizationInvitationRepository,
    private val permissionService: PermissionService
) {
    fun createInvitation(issuer: User, organizationId: Long, usernameToInvite: String, role: ScopeRole): InvitationCreatedResponse {
        if (role == ScopeRole.ORG_OWNER) {
            throw StatusCodeException(400, "Only one owner is allowed")
        }

        val organization = organizationRepository.findByIdOr404(organizationId)
        if (organization.type == OrganizationType.PERSONAL) {
            throw StatusCodeException(400, "Can not invite users to a Personal organization")
        }

        val userToInvite = userRepository.findByUsernameIgnoreCaseOr404(usernameToInvite)
        userScopeRepository.findByUserAndScopeId(userToInvite, organizationId).ifPresent {
            throw StatusCodeException(400, "Can not invite organization member")
        }

        requirePermission(
            permissionService.canSendOrganizationInvitation(issuer) {
                userScopeRepository.findByUserAndScopeId(issuer, organizationId)
            },
            { "Access denied: insufficient permissions." }
        )

        val invitation = organizationInvitationRepository.save(createInvitation(issuer, userToInvite, organization, role))
        // PROPOSAL: Notify of invitation?
        return InvitationCreatedResponse(invitation.id)
    }

    fun getInvitation(issuer: User, invitationId: Long): Map<String, Any?> {
        val organizationInvitation = organizationInvitationRepository.findByIdOr404(invitationId)
        return organizationInvitation.toMap()
    }

    fun resolveInvitation(userAcceptingInvitation: User, invitationId: Long, newStatus: InvitationStatus) {
        val organizationInvitation = organizationInvitationRepository.findByIdOr404(invitationId)

        if (organizationInvitation.status != InvitationStatus.PENDING) {
            throw StatusCodeException(400, "This invitation has already been resolved")
        }

        if (organizationInvitation.invitedUser.id != userAcceptingInvitation.id) {
            throw StatusCodeException(403, "Access denied: invitation is for another user")
        }

        organizationInvitation.status = newStatus
        organizationInvitation.answeredAt = Instant.now()
        organizationInvitationRepository.save(organizationInvitation)

        if (newStatus == InvitationStatus.ACCEPTED) {
            val organization = organizationInvitation.organization
            userScopeRepository.save(
                UserScope(
                    0,
                    userAcceptingInvitation,
                    ScopeType.ORGANIZATION,
                    organization.id,
                    organizationInvitation.role,
                    Instant.now()
                )
            )
        }
    }

    fun createInvitation(issuer: User, userToInvite: User, organization: Organization, role: ScopeRole) =
        OrganizationInvitation(
            0,
            issuer,
            userToInvite,
            organization,
            InvitationStatus.PENDING,
            role,
            Instant.now()
        )
}