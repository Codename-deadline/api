package xyz.om3lette.deadlines_api.services

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import xyz.om3lette.deadlines_api.data.common.constraints.DatabaseConstraint
import org.springframework.transaction.annotation.Transactional
import xyz.om3lette.deadlines_api.data.common.response.PaginationResponse
import xyz.om3lette.deadlines_api.data.scopes.organization.enums.InvitationStatus
import xyz.om3lette.deadlines_api.data.scopes.organization.enums.OrganizationType
import xyz.om3lette.deadlines_api.data.scopes.organization.model.Organization
import xyz.om3lette.deadlines_api.data.scopes.organization.model.OrganizationInvitation
import xyz.om3lette.deadlines_api.data.scopes.organization.repo.OrganizationInvitationRepository
import xyz.om3lette.deadlines_api.data.scopes.organization.repo.OrganizationRepository
import xyz.om3lette.deadlines_api.data.scopes.organization.response.OrganizationInvitationResponse
import xyz.om3lette.deadlines_api.data.scopes.organization.response.OrganizationInvitationsPendingResponse
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
import xyz.om3lette.deadlines_api.util.jpaRepository.violatesConstraint
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
    @Transactional
    fun createInvitation(issuer: User, organizationId: Long, usernameToInvite: String, role: ScopeRole): InvitationCreatedResponse {
        if (role == ScopeRole.ORG_OWNER || !role.canBeAssignedInScope(ScopeType.ORGANIZATION)) {
            throw StatusCodeException(400, ErrorCode.INVITATION_INVALID_ROLE)
        }

        val organization = organizationRepository.findByIdForUpdate(organizationId)
            ?: throw StatusCodeException(404, ErrorCode.ORG_NOT_FOUND)
        if (organization.type == OrganizationType.PERSONAL) {
            throw StatusCodeException(400, ErrorCode.INVITATION_PERSONAL_ORG)
        }

        val userToInvite = userRepository.findByUsernameIgnoreCaseOr404(usernameToInvite)
        if (userScopeRepository.existsByUserAndScopeIdAndScopeType(
            userToInvite, organizationId, ScopeType.ORGANIZATION
        )) {
            throw StatusCodeException(400, ErrorCode.INVITATION_ALREADY_ORG_MEMBER)
        }

        requirePermission(
            permissionService.canSendOrganizationInvitation(issuer, organizationId)
        )

        val invitation = try {
            organizationInvitationRepository.saveAndFlush(newPendingInvitation(issuer, userToInvite, organization, role))
        } catch (error: DataIntegrityViolationException) {
            if (!error.violatesConstraint(DatabaseConstraint.UQ_ORGANIZATION_INVITATIONS_PENDING)) throw error
            throw StatusCodeException(400, ErrorCode.INVITATION_ALREADY_INVITED)
        }

        // PROPOSAL: Notify of invitation?
        return InvitationCreatedResponse(invitation.id)
    }

    fun getInvitation(issuer: User, invitationId: Long): OrganizationInvitationResponse {
        val invitation = organizationInvitationRepository.findByIdOr404(invitationId, ErrorCode.INVITATION_NOT_FOUND)
        requirePermission(
            permissionService.canAccessOrganizationInvitation(issuer, invitation)
        )
        return invitation.toResponse()
    }

    fun revokeInvitation(issuer: User, invitationId: Long) {
        val invitation = organizationInvitationRepository.findByIdOr404(invitationId, ErrorCode.INVITATION_NOT_FOUND)
        requirePermission(
            permissionService.canAccessOrganizationInvitation(issuer, invitation)
        )
        invitation.status = InvitationStatus.REVOKED
        organizationInvitationRepository.save(invitation)
    }

    fun getPendingInvitationsByUser(
        issuer: User, pageNumber: Int, pageSize: Int
    ): PaginationResponse<OrganizationInvitationResponse> =
        PaginationResponse.fromPage(
            organizationInvitationRepository.findAllPendingByInvitedUserId(
                issuer.id, PageRequest.of(pageNumber, pageSize)
            ).map { it.toResponse() }
        )

    fun getPendingSentInvitationsByUser(
        issuer: User, pageNumber: Int, pageSize: Int
    ): PaginationResponse<OrganizationInvitationResponse> =
        PaginationResponse.fromPage(
            organizationInvitationRepository.findAllPendingSentByUserId(
                issuer.id, PageRequest.of(pageNumber, pageSize)
            ).map { it.toResponse() }
        )

    fun getNumberOfPendingInvitationsByUser(issuer: User) =
        OrganizationInvitationsPendingResponse(
            pending = organizationInvitationRepository.countPendingByInvitedUserId(issuer.id)
        )

    @Transactional
    fun resolveInvitation(userAcceptingInvitation: User, invitationId: Long, newStatus: InvitationStatus) {
        val organizationInvitation = organizationInvitationRepository.findByIdOr404(
            invitationId, ErrorCode.INVITATION_NOT_FOUND
        )

        if (organizationInvitation.invitedUser.id != userAcceptingInvitation.id) {
            throw StatusCodeException(403, ErrorCode.AUTH_INSUFFICIENT_PERMISSIONS)
        }
        if (organizationInvitation.status != InvitationStatus.PENDING) {
            throw StatusCodeException(400, ErrorCode.INVITATION_ALREADY_ANSWERED)
        }

        val organization = if (newStatus == InvitationStatus.ACCEPTED) {
            organizationRepository.findByIdForUpdate(organizationInvitation.organization.id)
                ?.also {
                    if (it.type == OrganizationType.PERSONAL) {
                        throw StatusCodeException(400, ErrorCode.INVITATION_PERSONAL_ORG)
                    }
                }
                ?: throw StatusCodeException(404, ErrorCode.ORG_NOT_FOUND)
        } else {
            null
        }

        organizationInvitation.status = newStatus
        organizationInvitation.answeredAt = Instant.now()
        organizationInvitationRepository.save(organizationInvitation)

        if (organization != null) {
            userScopeRepository.save(
                UserScope(
                    userAcceptingInvitation,
                    ScopeType.ORGANIZATION,
                    organization.id,
                    organizationInvitation.role,
                    Instant.now()
                )
            )
        }
    }

    fun newPendingInvitation(issuer: User, userToInvite: User, organization: Organization, role: ScopeRole) =
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
