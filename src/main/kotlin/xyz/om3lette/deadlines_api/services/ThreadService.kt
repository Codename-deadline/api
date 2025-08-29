package xyz.om3lette.deadlines_api.services

import jakarta.transaction.Transactional
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import xyz.om3lette.deadlines_api.data.scopes.organization.repo.OrganizationRepository
import xyz.om3lette.deadlines_api.data.scopes.thread.model.Thread
import xyz.om3lette.deadlines_api.data.scopes.thread.repo.ThreadRepository
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeRole
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeType
import xyz.om3lette.deadlines_api.data.scopes.userScope.model.UserScope
import xyz.om3lette.deadlines_api.data.scopes.userScope.repo.UserScopeRepository
import xyz.om3lette.deadlines_api.util.user.isAdminOr
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.data.user.repo.UserRepository
import xyz.om3lette.deadlines_api.exceptions.type.StatusCodeException
import xyz.om3lette.deadlines_api.services.permission.PermissionLookupService
import xyz.om3lette.deadlines_api.services.permission.PermissionService
import xyz.om3lette.deadlines_api.util.MessageResponse
import xyz.om3lette.deadlines_api.util.jpaRepository.findByIdOr404
import xyz.om3lette.deadlines_api.util.requirePermission
import xyz.om3lette.deadlines_api.util.userRepository.findByUsernameIgnoreCaseOr404
import java.time.Instant

@Service
class ThreadService(
    private val userRepository: UserRepository,
    private val userScopeRepository: UserScopeRepository,
    private val threadRepository: ThreadRepository,
    private val organizationRepository: OrganizationRepository,
    private val permissionService: PermissionService,
    private val permissionLookupService: PermissionLookupService
) {

    fun createThread(
        issuer: User,
        organizationId: Long,
        title: String,
        description: String?,
        assigneesUsernames: List<String>
    ): MessageResponse {
        requirePermission(
            permissionService.canCreateOrDeleteThread(issuer) {
                userScopeRepository.findByUserAndScopeId(issuer, organizationId)
            }
        )

        val organization = organizationRepository.findByIdOr404(organizationId)
        val thread = threadRepository.save(
            Thread(
                0, title, description, organization, Instant.now()
            )
        )

        val threadAssigneeScopes: MutableList<UserScope> = mutableListOf()

        userScopeRepository.findByScopeIdAndUsernameInIgnoreCase(
            organization.id,
            assigneesUsernames.map { it.lowercase() }
        ).forEach { userScope ->
//          RETHINK
//          If a user is an admin he has the access anyway => don't add another entry
            if (
                userScope.user.isAdminOr {
                    userScope.role.isEqualOrHigherThan(ScopeRole.ORG_ADMIN)
                }
            ) return@forEach

            threadAssigneeScopes.add(
                UserScope(
                    0,
                    userScope.user,
                    ScopeType.THREAD,
                    thread.id,
                    ScopeRole.THR_ASSIGNEE,
                    Instant.now()
                )
            )
        }
        userScopeRepository.saveAll(threadAssigneeScopes)
        return MessageResponse.success(mapOf("threadId" to thread.id))
    }

    fun deleteThread(issuer: User, threadId: Long): MessageResponse {
        val (thread, issuerScope) = permissionLookupService.getThreadAndHighestRoleUserScopeOr404(issuer, threadId)
        requirePermission(
            permissionService.canCreateOrDeleteThread(issuer, issuerScope)
        )

        threadRepository.delete(thread)

        return MessageResponse.success("Thread deleted")
    }

    @Transactional
    fun removeAssignee(issuer: User, threadId: Long, assigneeUsername: String): MessageResponse {
        if (assigneeUsername.equals(issuer.username, ignoreCase = true)) {
            throw StatusCodeException(400, "Removing yourself is prohibited")
        }
        val (_, issuerScope) = permissionLookupService.getThreadAndHighestRoleUserScopeOr404(issuer, threadId)

        requirePermission(
            permissionService.canManageThreadAssignees(issuer, issuerScope)
        )

        val userToRemove = userRepository.findByUsernameIgnoreCaseOr404(assigneeUsername)
        userScopeRepository.deleteByUserAndScopeId(userToRemove, threadId)

//      Deadline scopes for the user do not exist as he was a THREAD_ASSIGNEE which already grants access
//      to all deadlines of the given thread

        return MessageResponse.success("Assignee removed")
    }

    fun getThreadMetaData(issuer: User, threadId: Long): MessageResponse {
        val (thread, issuerScope) = permissionLookupService.getThreadAndHighestRoleUserScopeOr404(issuer, threadId)

        requirePermission(
            permissionService.hasThreadAccess(
                issuer, issuerScope, thread.organization
            )
        )

        return MessageResponse.success(thread.toMap())
    }

    fun getThreadsByOrganization(
        issuer: User,
        organizationId: Long,
        pageNumber: Int,
        pageSize: Int
    ): MessageResponse {
        val organization = organizationRepository.findByIdOr404(organizationId)

        requirePermission(
            permissionService.hasOrganizationAccess(issuer, organization) {
                userScopeRepository.findByUserAndScopeId(issuer, organization.id)
            }
        )

        val pageRequest = PageRequest.of(pageNumber, pageSize)
        return MessageResponse.success(threadRepository.findAllByOrganization(organization, pageRequest).map { it.toMap() })
    }

    fun patchThread(issuer: User, threadId: Long, title: String?, description: String?): MessageResponse {
        if (title == null && description == null) return MessageResponse.success("Update unnecessary")

        val (thread, issuerScope) = permissionLookupService.getThreadAndHighestRoleUserScopeOr404(issuer, threadId)

        requirePermission(
            permissionService.canUpdateThread(issuer, issuerScope)
        )

        if (title != null) thread.title = title
        if (description != null) thread.description = description

        threadRepository.save(thread)

        return MessageResponse.success("Update successful")
    }

    fun getThreadAssignees(
        issuer: User,
        threadId: Long,
        pageNumber: Int,
        pageSize: Int
    ): MessageResponse {
        val (thread, issuerScope) = permissionLookupService.getThreadAndHighestRoleUserScopeOr404(issuer, threadId)

        requirePermission(
            permissionService.hasThreadAccess(issuer, issuerScope, thread.organization)
        )

        val pageRequest = PageRequest.of(pageNumber, pageSize, Sort.by("role").descending())
        return MessageResponse.success(
            userScopeRepository.findAllByScopeId(threadId, pageRequest).map { it.toMap() }
        )
    }
}