package xyz.om3lette.deadlines_api.services

import jakarta.transaction.Transactional
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import xyz.om3lette.deadlines_api.data.common.response.PaginationResponse
import xyz.om3lette.deadlines_api.data.permissions.dto.OrganizationScope
import xyz.om3lette.deadlines_api.data.permissions.dto.ThreadScope
import xyz.om3lette.deadlines_api.data.scopes.organization.repo.OrganizationRepository
import xyz.om3lette.deadlines_api.data.scopes.thread.dto.ThreadStatsDTO
import xyz.om3lette.deadlines_api.data.scopes.thread.model.Thread
import xyz.om3lette.deadlines_api.data.scopes.thread.repo.ThreadRepository
import xyz.om3lette.deadlines_api.data.scopes.thread.response.ThreadCreatedResponse
import xyz.om3lette.deadlines_api.data.scopes.thread.response.ThreadResponse
import xyz.om3lette.deadlines_api.data.scopes.thread.response.ThreadResponseWithRole
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeRole
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeType
import xyz.om3lette.deadlines_api.data.scopes.userScope.model.UserScope
import xyz.om3lette.deadlines_api.data.scopes.userScope.repo.UserScopeRepository
import xyz.om3lette.deadlines_api.data.scopes.userScope.response.UserScopeResponse
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.data.user.repo.UserRepository
import xyz.om3lette.deadlines_api.exceptions.enums.ErrorCode
import xyz.om3lette.deadlines_api.exceptions.type.StatusCodeException
import xyz.om3lette.deadlines_api.services.permission.PermissionContext
import xyz.om3lette.deadlines_api.services.permission.PermissionService
import xyz.om3lette.deadlines_api.util.jpaRepository.findByIdOr404
import xyz.om3lette.deadlines_api.util.page.toPaginationResponse
import xyz.om3lette.deadlines_api.util.requirePermission
import xyz.om3lette.deadlines_api.util.userRepository.findByUsernameIgnoreCaseOr404
import java.time.Instant

@Service
class ThreadService(
    private val userRepository: UserRepository,
    private val userScopeRepository: UserScopeRepository,
    private val threadRepository: ThreadRepository,
    private val organizationRepository: OrganizationRepository,
    private val permissionService: PermissionService
) {

    fun createThread(
        issuer: User,
        organizationId: Long,
        title: String,
        description: String?,
        assigneesUsernames: List<String>
    ): ThreadCreatedResponse {
        requirePermission(
            permissionService.canCreateThread(issuer, organizationId)
        )

        val organization = organizationRepository.findByIdOr404(organizationId, ErrorCode.ORG_NOT_FOUND)

        val creationTime = Instant.now()
        val thread = threadRepository.save(
            Thread(
                0, title, description, organization, creationTime
            )
        )

        // Start with a thread creator and then add all the assignees
        val threadAssigneeScopes: MutableList<UserScope> = mutableListOf(
            UserScope(
                0,
                issuer,
                ScopeType.THREAD,
                thread.id,
                ScopeRole.THR_OWNER,
                creationTime
            )
        )
        userScopeRepository.findByScopeIdAndScopeTypeAndUsernameInIgnoreCase(
            organization.id, ScopeType.ORGANIZATION,
            assigneesUsernames.map { it.lowercase() }
        ).forEach { userScope ->
            threadAssigneeScopes.add(
                UserScope(
                    0,
                    userScope.user,
                    ScopeType.THREAD,
                    thread.id,
                    ScopeRole.THR_ASSIGNEE,
                    creationTime
                )
            )
        }

        userScopeRepository.saveAll(threadAssigneeScopes)
        return ThreadCreatedResponse(thread.id, threadAssigneeScopes.size)
    }

    fun deleteThread(issuer: User, threadId: Long) {
        val thread = threadRepository.findByIdOr404(threadId, ErrorCode.THR_NOT_FOUND)
        requirePermission(
            permissionService.canDelete(issuer, ThreadScope(thread))
        )

        threadRepository.delete(thread)
    }

    fun addAssignee(issuer: User, threadId: Long, username: String, role: ScopeRole) {
        if (!role.name.startsWith("THR")) {
            throw StatusCodeException(400, ErrorCode.INVITATION_INVALID_ROLE)
        }
        if (username.equals(issuer.username, ignoreCase = true)) {
            throw StatusCodeException(400, ErrorCode.INVITATION_SELF_INVITE)
        }

        val thread = threadRepository.findByIdOr404(threadId, ErrorCode.THR_NOT_FOUND)
        val newAssignee = userScopeRepository.findByScopeTypeAndScopeIdAndUsernameIgnoreCase(
            username, ScopeType.ORGANIZATION, thread.organization.id
        ).orElseThrow{ StatusCodeException(400, ErrorCode.INVITATION_NOT_ORG_MEMBER) }
        requirePermission(
            permissionService.canManageAssignees(issuer, ThreadScope(thread))
        )

        userScopeRepository.save(
            UserScope(
                0,
                newAssignee.user,
                ScopeType.THREAD,
                thread.id,
                role,
                Instant.now()
            )
        )
    }

    @Transactional
    fun removeAssignee(issuer: User, threadId: Long, assigneeUsername: String) {
        if (assigneeUsername.equals(issuer.username, ignoreCase = true)) {
            throw StatusCodeException(400, ErrorCode.ACTION_SELF_REMOVAL)
        }

        requirePermission(
            permissionService.canManageAssignees(issuer, ThreadScope(
                threadRepository.findByIdOr404(threadId, ErrorCode.THR_NOT_FOUND)
            ))
        )

        // FIXME: RETHINK
        // Deadline scopes for the user do not exist as he was a THREAD_ASSIGNEE which already grants access
        // to all deadlines of the given thread
        val userToRemove = userRepository.findByUsernameIgnoreCaseOr404(assigneeUsername)
        userScopeRepository.deleteByUserAndScopeId(userToRemove, null, threadId, null)
    }

    fun getThread(issuer: User, threadId: Long): ThreadResponse {
        val thread: Thread = threadRepository.findByIdOr404(threadId, ErrorCode.THR_NOT_FOUND)

        requirePermission(
            permissionService.hasAccess(issuer, ThreadScope(thread))
        )

        val stats = threadRepository.getThreadStats(listOf(thread.id))[0]
        return thread.toResponse(
            stats,
            permissionService.buildThreadPermissions(issuer, thread)
        )
    }

    private fun prepareThreadResponseData(user: User, threadIds: List<Long>, prefetchRoles: Boolean = true): Map<Long, ThreadStatsDTO> {
        if (prefetchRoles) {
            permissionService.prefetchUserRoles(user, thrIds = threadIds)
        }

        return threadRepository.getThreadStats(threadIds)
            .associateBy { it.threadId }
    }

    private fun mapThreadToFullResponse(user: User, thread: Thread, stats: Map<Long, ThreadStatsDTO>) =
        thread.toResponse(stats[thread.id]!!, permissionService.buildThreadPermissions(user, thread)).withRole(
            permissionService.getRole(thread.id, ScopeType.THREAD),
            permissionService.getMaxRole(
                listOf(
                    PermissionContext.PermissionKey(ScopeType.THREAD, thread.id),
                    PermissionContext.PermissionKey(ScopeType.ORGANIZATION, thread.organization.id)
                )
            ).takeIf {
                // TODO: PermissionService might be useful
                // The goal is to not return a "read only" role
                    maxRole -> maxRole > ScopeRole.THR_ASSIGNEE
            }
        )

    fun getThreadsByUser(
        issuer: User,
        pageNumber: Int,
        pageSize: Int
    ): PaginationResponse<ThreadResponseWithRole> {
        val threadIds = userScopeRepository.findAllScopeIdsByUserAndScopeType(
            issuer.id, ScopeType.THREAD, PageRequest.of(pageNumber, pageSize)
        )

        val stats = prepareThreadResponseData(issuer, threadIds.toList())
        return PaginationResponse(
            threadRepository.findAllById(threadIds).map {
                mapThreadToFullResponse(issuer, it, stats)
            },
            totalPages = threadIds.totalPages
        )
    }

    fun getThreadsByOrganization(
        issuer: User,
        organizationId: Long,
        pageNumber: Int,
        pageSize: Int
    ): PaginationResponse<ThreadResponseWithRole> {
        val organization = organizationRepository.findByIdOr404(organizationId, ErrorCode.ORG_NOT_FOUND)
        requirePermission(
            permissionService.hasAccess(issuer, OrganizationScope(
                organizationId, organization
            ))
        )

        val threads = threadRepository.findAllByOrganization(
            organization, PageRequest.of(pageNumber, pageSize)
        )
        val stats = prepareThreadResponseData(issuer, threads.map { it.id }.toList())

        return threads.toPaginationResponse {
            mapThreadToFullResponse(issuer, it, stats)
        }
    }

    fun patchThread(issuer: User, threadId: Long, title: String?, description: String?) {
        if (title == null && description == null) {
            return
        }

        val thread: Thread = threadRepository.findByIdOr404(threadId, ErrorCode.THR_NOT_FOUND)
        requirePermission(
            permissionService.canUpdate(issuer, ThreadScope(thread))
        )

        if (title != null) thread.title = title
        if (description != null) thread.description = description

        threadRepository.save(thread)
    }

    fun getThreadAssignees(
        issuer: User,
        threadId: Long,
        pageNumber: Int,
        pageSize: Int
    ): PaginationResponse<UserScopeResponse> {
        val thread: Thread = threadRepository.findByIdOr404(threadId, ErrorCode.THR_NOT_FOUND)

        requirePermission(
            permissionService.hasAccess(issuer, ThreadScope(thread))
        )

        val pageRequest = PageRequest.of(pageNumber, pageSize, Sort.by("role").descending())
        return userScopeRepository.findAllByScopeIdAndScopeType(
            threadId, ScopeType.THREAD, pageRequest
        ).toPaginationResponse { it.toResponse() }
    }
}