package xyz.om3lette.deadlines_api.services

import jakarta.transaction.Transactional
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import xyz.om3lette.deadlines_api.data.common.response.PaginationResponse
import xyz.om3lette.deadlines_api.data.permissions.dto.OrganizationScope
import xyz.om3lette.deadlines_api.data.permissions.dto.ThreadScope
import xyz.om3lette.deadlines_api.data.scopes.organization.repo.OrganizationRepository
import xyz.om3lette.deadlines_api.data.scopes.thread.model.Thread
import xyz.om3lette.deadlines_api.data.scopes.thread.repo.ThreadRepository
import xyz.om3lette.deadlines_api.data.scopes.thread.response.ThreadCreatedResponse
import xyz.om3lette.deadlines_api.data.scopes.thread.response.ThreadResponse
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeRole
import xyz.om3lette.deadlines_api.data.scopes.userScope.enums.ScopeType
import xyz.om3lette.deadlines_api.data.scopes.userScope.model.UserScope
import xyz.om3lette.deadlines_api.data.scopes.userScope.repo.UserScopeRepository
import xyz.om3lette.deadlines_api.data.scopes.userScope.response.UserScopeResponse
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.data.user.repo.UserRepository
import xyz.om3lette.deadlines_api.exceptions.enums.ErrorCode
import xyz.om3lette.deadlines_api.exceptions.type.StatusCodeException
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
        val thread = threadRepository.save(
            Thread(
                0, title, description, organization, Instant.now()
            )
        )

        val threadAssigneeScopes: MutableList<UserScope> = mutableListOf()

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
                    Instant.now()
                )
            )
        }
        userScopeRepository.saveAll(threadAssigneeScopes)
        return ThreadCreatedResponse(thread.id)
    }

    fun deleteThread(issuer: User, threadId: Long) {
        val thread = threadRepository.findByIdOr404(threadId, ErrorCode.THR_NOT_FOUND)
        requirePermission(
            permissionService.canDelete(issuer, ThreadScope(thread))
        )

        threadRepository.delete(thread)
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

    fun getThreadMetaData(issuer: User, threadId: Long): ThreadResponse {
        val thread: Thread = threadRepository.findByIdOr404(threadId, ErrorCode.THR_NOT_FOUND)

        requirePermission(
            permissionService.hasAccess(issuer, ThreadScope(thread))
        )

        return thread.toResponse()
    }

    fun getThreadsByOrganization(
        issuer: User,
        organizationId: Long,
        pageNumber: Int,
        pageSize: Int
    ): PaginationResponse<ThreadResponse> {
        val organization = organizationRepository.findByIdOr404(organizationId, ErrorCode.ORG_NOT_FOUND)

        requirePermission(
            permissionService.hasAccess(issuer, OrganizationScope(
                organizationId, organization
            ))
        )

        val pageRequest = PageRequest.of(pageNumber, pageSize)
        return threadRepository.findAllByOrganization(
            organization, pageRequest
        ).toPaginationResponse { it.toResponse() }
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