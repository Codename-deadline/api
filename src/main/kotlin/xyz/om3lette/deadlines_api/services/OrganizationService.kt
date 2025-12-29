package xyz.om3lette.deadlines_api.services

import org.springframework.stereotype.Service
import xyz.om3lette.deadlines_api.data.scopes.organization.enums.OrganizationType
import xyz.om3lette.deadlines_api.data.scopes.organization.model.Organization
import xyz.om3lette.deadlines_api.data.scopes.organization.model.OrganizationInvitation
import xyz.om3lette.deadlines_api.data.scopes.organization.repo.OrganizationInvitationRepository
import xyz.om3lette.deadlines_api.data.scopes.organization.repo.OrganizationRepository
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.data.user.repo.UserRepository
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeRole
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeType
import xyz.om3lette.deadlines_api.data.scopes.userScope.model.UserScope
import xyz.om3lette.deadlines_api.data.scopes.userScope.repo.UserScopeRepository
import xyz.om3lette.deadlines_api.exceptions.type.StatusCodeException
import xyz.om3lette.deadlines_api.util.jpaRepository.findByIdOr404
import xyz.om3lette.deadlines_api.util.userRepository.findByUsernameIgnoreCaseOr404
import java.time.Instant
import jakarta.transaction.Transactional
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import xyz.om3lette.deadlines_api.data.scopes.deadline.repo.DeadlineRepository
import xyz.om3lette.deadlines_api.data.scopes.organization.model.InvitationDTO
import xyz.om3lette.deadlines_api.data.scopes.organization.response.OrganizationCreatedResponse
import xyz.om3lette.deadlines_api.data.scopes.organization.response.OrganizationResponse
import xyz.om3lette.deadlines_api.data.scopes.thread.repo.ThreadRepository
import xyz.om3lette.deadlines_api.data.scopes.userScope.response.UserScopeResponse
import xyz.om3lette.deadlines_api.exceptions.enums.ErrorCode
import xyz.om3lette.deadlines_api.services.permission.PermissionService
import xyz.om3lette.deadlines_api.util.requirePermission

@Service
class OrganizationService(
    private val userRepository: UserRepository,
    private val userScopeRepository: UserScopeRepository,
    private val threadRepository: ThreadRepository,
    private val deadlineRepository: DeadlineRepository,
    private val organizationRepository: OrganizationRepository,
    private val organizationInvitationRepository: OrganizationInvitationRepository,
    private val permissionService: PermissionService,
    private val organizationInvitationService: OrganizationInvitationService,
) {
    fun createOrganization(
        issuer: User,
        title: String,
        description: String?,
        type: OrganizationType,
        usernameRolePairToInvite: List<InvitationDTO>
    ): OrganizationCreatedResponse {
        val organization = organizationRepository.save(
            Organization(
                0, title, description, type, Instant.now()
            )
        )

        userScopeRepository.save(
            UserScope(
                0,
                issuer,
                ScopeType.ORGANIZATION,
                organization.id,
                ScopeRole.ORG_OWNER,
                Instant.now()
            )
        )

        val invitations: MutableList<OrganizationInvitation> = mutableListOf()
        val usernameToInvitation: Map<String, InvitationDTO> = usernameRolePairToInvite.associateBy {
            it.username.lowercase()
        }
        userRepository.findByUsernameInIgnoreCase(usernameToInvitation.keys.toList()).forEach { user ->
//          Usernames which were used in a db lookup are the keys of the map -> they exist in the map
            val usernameRolePair = usernameToInvitation[user.username.lowercase()]!!
            if (usernameRolePair.toScopeRole() == ScopeRole.ORG_OWNER) return@forEach

            invitations.add(
                organizationInvitationService.createInvitation(
                    issuer,
                    user,
                    organization,
                    usernameRolePair.toScopeRole()
                )
            )
        }
        organizationInvitationRepository.saveAll(invitations)
        return OrganizationCreatedResponse(organization.id)
    }

    fun deleteOrganization(issuer: User, organizationId: Long) {
        val organization: Organization = organizationRepository.findByIdOr404(organizationId, ErrorCode.ORG_NOT_FOUND)
        requirePermission(
            permissionService.canDeleteOrganization(issuer) {
                userScopeRepository.findByUserAndScopeId(issuer, organizationId)
            }
        )

        organizationRepository.delete(organization)
    }

    @Transactional
    fun removeMember(issuer: User, organizationId: Long, memberUsernameToRemove: String) {
        if (memberUsernameToRemove.equals(issuer._username, ignoreCase = true)) {
            throw StatusCodeException(400, ErrorCode.ACTION_SELF_REMOVAL)
        }

        requirePermission(
            permissionService.canManageOrganizationMembers(issuer) {
                userScopeRepository.findByUserAndScopeId(issuer, organizationId)
            }
        )

        val userToRemove = userRepository.findByUsernameIgnoreCaseOr404(memberUsernameToRemove)
        userScopeRepository.deleteByUserAndScopeId(userToRemove, organizationId)

        val threadIds = threadRepository.findAllIdsByOrganizationId(organizationId)
        if (threadIds.isNotEmpty()) {
            userScopeRepository.deleteByUserAndScopeIdIn(userToRemove, threadIds)
        }

        val deadlinesIds = deadlineRepository.findAllIdsByOrganizationId(organizationId)
        if (deadlinesIds.isNotEmpty()) {
            userScopeRepository.deleteByUserAndScopeIdIn(userToRemove, deadlinesIds)
        }
    }

    fun getOrganizationMetaData(issuer: User, organizationId: Long): OrganizationResponse {
        val organization = organizationRepository.findByIdOr404(organizationId, ErrorCode.ORG_NOT_FOUND)
        requirePermission(
            permissionService.hasOrganizationAccess(issuer, organization) {
                userScopeRepository.findByUserAndScopeId(issuer, organization.id)
            }
        )

        return organization.toResponse()
    }

    fun patchOrganization(issuer: User, organizationId: Long, title: String?, description: String?) {
        if (title == null && description == null) {
            return
        }

        val organization: Organization = organizationRepository.findByIdOr404(organizationId, ErrorCode.ORG_NOT_FOUND)

        requirePermission(
            permissionService.canUpdateOrganization(issuer) {
                userScopeRepository.findByUserAndScopeId(issuer, organizationId)
            }
        )

        if (title != null) organization.title = title
        if (description != null) organization.description = description

        organizationRepository.save(organization)
    }

    fun getOrganizationMembers(
        issuer: User,
        organizationId: Long,
        pageNumber: Int,
        pageSize: Int
    ): List<UserScopeResponse> {
        val organization = organizationRepository.findByIdOr404(organizationId, ErrorCode.ORG_NOT_FOUND)
        requirePermission(
            permissionService.hasOrganizationAccess(issuer, organization) {
                userScopeRepository.findByUserAndScopeId(issuer, organization.id)
            }
        )

        val pageRequest = PageRequest.of(pageNumber, pageSize, Sort.by("role").descending())
        return userScopeRepository.findAllByScopeId(organizationId, pageRequest).map { member ->
            member.toResponse()
        }
    }
}
