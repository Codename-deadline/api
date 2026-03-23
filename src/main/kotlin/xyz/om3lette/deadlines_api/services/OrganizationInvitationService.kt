package xyz.om3lette.deadlines_api.services

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import xyz.om3lette.deadlines_api.data.scopes.organization.enums.InvitationStatus
import xyz.om3lette.deadlines_api.data.scopes.organization.enums.OrganizationType
import xyz.om3lette.deadlines_api.data.scopes.organization.model.Organization
import xyz.om3lette.deadlines_api.data.scopes.organization.model.OrganizationInvitation
import xyz.om3lette.deadlines_api.data.scopes.organization.repo.OrganizationInvitationRepository
import xyz.om3lette.deadlines_api.data.scopes.organization.repo.OrganizationRepository
import xyz.om3lette.deadlines_api.data.scopes.organization.response.OrganizationInvitationResponse
import xyz.om3lette.deadlines_api.data.scopes.organization.response.member.InvitationCreatedResponse
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeRole
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeType
import xyz.om3lette.deadlines_api.data.scopes.userScope.model.UserScope
import xyz.om3lette.deadlines_api.data.scopes.userScope.repo.UserScopeRepository
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.data.user.repo.UserRepository
import xyz.om3lette.deadlines_api.exceptions.enums.ErrorCode
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
        // TODO: Introduce a narrow organization role type or move to payload validation
        if (role == ScopeRole.ORG_OWNER || !role.name.startsWith("ORG")) {
            throw StatusCodeException(400, ErrorCode.INVITATION_INVALID_ROLE)
        }

        val organization = organizationRepository.findByIdOr404(organizationId, ErrorCode.ORG_NOT_FOUND)
        if (organization.type == OrganizationType.PERSONAL) {
            throw StatusCodeException(400, ErrorCode.INVITATION_PERSONAL_ORG)
        }

        val userToInvite = userRepository.findByUsernameIgnoreCaseOr404(usernameToInvite)
        // FIXME: Replace by exists
        userScopeRepository.findByUserAndScopeIdAndScopeType(
            userToInvite, organizationId, ScopeType.ORGANIZATION
        ).ifPresent {
            throw StatusCodeException(400, ErrorCode.INVITATION_ALREADY_ORG_MEMBER)
        }

        requirePermission(
            permissionService.canSendOrganizationInvitation(issuer, organizationId)
        )

        // FIXME: Add a db partial index to disallow multiple pending invitations of the same user at the same time
        val invitation = try {
            organizationInvitationRepository.save(createInvitation(issuer, userToInvite, organization, role))
        } catch (_: DataIntegrityViolationException) {
            throw StatusCodeException(400, ErrorCode.INVITATION_ALREADY_INVITED)
        }

        // PROPOSAL: Notify of invitation?
        return InvitationCreatedResponse(invitation.id)
    }

    fun getInvitation(issuer: User, invitationId: Long): OrganizationInvitationResponse =
        organizationInvitationRepository.findByIdOr404(invitationId, ErrorCode.INVITATION_NOT_FOUND).toResponse()

    fun resolveInvitation(userAcceptingInvitation: User, invitationId: Long, newStatus: InvitationStatus) {
        val organizationInvitation = organizationInvitationRepository.findByIdOr404(
            invitationId, ErrorCode.INVITATION_NOT_FOUND
        )

        if (organizationInvitation.status != InvitationStatus.PENDING) {
            throw StatusCodeException(400, ErrorCode.INVITATION_ALREADY_ANSWERED)
        }

        if (organizationInvitation.invitedUser.id != userAcceptingInvitation.id) {
            throw StatusCodeException(403, ErrorCode.AUTH_INSUFFICIENT_PERMISSIONS)
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