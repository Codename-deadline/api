package xyz.om3lette.deadlines_api.services

import jakarta.transaction.Transactional
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import xyz.om3lette.deadlines_api.data.common.response.PaginationResponse
import xyz.om3lette.deadlines_api.data.notifications.enums.NotificationStatus
import xyz.om3lette.deadlines_api.data.notifications.enums.TimeRemaining
import xyz.om3lette.deadlines_api.data.notifications.model.DeadlineNotification
import xyz.om3lette.deadlines_api.data.notifications.repo.DeadlineNotificationRepository
import xyz.om3lette.deadlines_api.data.permissions.dto.DeadlineScope
import xyz.om3lette.deadlines_api.data.permissions.dto.ThreadScope
import xyz.om3lette.deadlines_api.data.scopes.common.dto.UsernameRolePairList
import xyz.om3lette.deadlines_api.data.scopes.deadline.dto.DeadlineStatsDTO
import xyz.om3lette.deadlines_api.data.scopes.deadline.model.Deadline
import xyz.om3lette.deadlines_api.data.scopes.deadline.repo.DeadlineRepository
import xyz.om3lette.deadlines_api.data.scopes.deadline.response.DeadlineCreatedResponse
import xyz.om3lette.deadlines_api.data.scopes.deadline.response.DeadlineResponse
import xyz.om3lette.deadlines_api.data.scopes.deadline.response.DeadlineResponseWithRole
import xyz.om3lette.deadlines_api.data.scopes.thread.repo.ThreadRepository
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
import xyz.om3lette.deadlines_api.util.user.isAdminOr
import xyz.om3lette.deadlines_api.util.userRepository.findByUsernameIgnoreCaseOr404
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class DeadlineService(
    private val minDeadlineExpiryTimeMinutes: Long = 15,
    private val userRepository: UserRepository,
    private val userScopeRepository: UserScopeRepository,
    private val threadRepository: ThreadRepository,
    private val deadlineRepository: DeadlineRepository,
    private val deadlineNotificationRepository: DeadlineNotificationRepository,
    private val permissionService: PermissionService
) {

    @Transactional
    fun createDeadline(
        issuer: User,
        threadId: Long,
        title: String,
        description: String?,
        due: Instant,
        assignees: UsernameRolePairList
    ): DeadlineCreatedResponse {
        val now = Instant.now()
        val minExpirationTime = now.plus(minDeadlineExpiryTimeMinutes, ChronoUnit.MINUTES)
        if (due.isBefore(minExpirationTime)) {
            val minutesBeforeExpiration = ChronoUnit.MINUTES.between(now, due)
            throw StatusCodeException(
                statusCode = 400,
                code = ErrorCode.DDL_INVALID_TIMESTAMP,
                detail = "Cannot create a deadline with $minutesBeforeExpiration minutes before expiration. Min value: $minDeadlineExpiryTimeMinutes",
                params = mapOf(
                    "remaining" to minutesBeforeExpiration,
                    "min" to minDeadlineExpiryTimeMinutes
                )
            )
        }
        val thread = threadRepository.findByIdOr404(threadId, ErrorCode.THR_NOT_FOUND)

        requirePermission(
            permissionService.canCreateDeadline(issuer, thread)
        )

        val creationTimestamp = Instant.now()
        val deadline = deadlineRepository.save(
            Deadline(
                id = 0,
                organization = thread.organization,
                thread = thread,
                title = title,
                description = description,
                createdAt = creationTimestamp,
                due = due
            )
        )

        val assigneeMap = assignees.filterByScope(ScopeType.DEADLINE).associateBy { it.username.lowercase() }
        val deadlineAssigneeScopes: MutableList<UserScope> = mutableListOf()
        userScopeRepository.findByScopeTypeScopeIdInAndUsernameInIgnoreCase(
            thread.organization.id,
            ScopeType.ORGANIZATION,
            assigneeMap.keys.map { it }
        )
            .groupBy { it.user.id }.values
            .map{ scopes -> scopes.maxBy { it.role } }
            .forEach { userScope ->
                deadlineAssigneeScopes.add(
                    UserScope(
                        0,
                        userScope.user,
                        ScopeType.DEADLINE,
                        deadline.id,
                        assigneeMap[userScope.user.username.lowercase()]!!.role,
                        creationTimestamp
                    )
                )
            }

        userScopeRepository.saveAll(deadlineAssigneeScopes)


        fun createNotification(amount: Long, timeUnit: ChronoUnit, type: TimeRemaining): DeadlineNotification? {
            val sendAt = when(timeUnit) {
                ChronoUnit.WEEKS -> due.minus(Duration.ofDays(7 * amount))
                ChronoUnit.MONTHS -> due.minus(Duration.ofSeconds(31556952L / 12))
                else -> due.minus(amount, timeUnit)
            }
            return if (sendAt.isAfter(now)) DeadlineNotification(
                0, deadline, sendAt, type, NotificationStatus.PENDING
            ) else null
        }

        val notifications: List<DeadlineNotification> = mutableListOf(
            createNotification(15, ChronoUnit.MINUTES, TimeRemaining.FIFTEEN_MINUTES),
            createNotification(1, ChronoUnit.HOURS, TimeRemaining.ONE_HOUR),
            createNotification(1, ChronoUnit.DAYS, TimeRemaining.ONE_DAY),
            createNotification(1, ChronoUnit.WEEKS, TimeRemaining.ONE_WEEK),
            createNotification(1, ChronoUnit.MONTHS, TimeRemaining.ONE_MONTH),
        ).filterNotNull()
        deadlineNotificationRepository.saveAll(notifications)

        return DeadlineCreatedResponse(deadline.id, deadlineAssigneeScopes.size)
    }

    fun deleteDeadline(issuer: User, deadlineId: Long) {
        val deadline = deadlineRepository.findByIdOr404(deadlineId, ErrorCode.DDL_NOT_FOUND)

        requirePermission(
            permissionService.canDelete(issuer, DeadlineScope(deadline))
        )

        deadlineRepository.delete(deadline)
    }

    fun addAssignee(issuer: User, deadlineId: Long, username: String, role: ScopeRole) {
        if (!role.canBeAssignedInScope(ScopeType.DEADLINE)) {
            throw StatusCodeException(400, ErrorCode.INVITATION_INVALID_ROLE)
        }
        if (username.equals(issuer.username, ignoreCase = true)) {
            throw StatusCodeException(400, ErrorCode.INVITATION_SELF_INVITE)
        }

        val deadline = deadlineRepository.findByIdOr404(deadlineId, ErrorCode.DDL_NOT_FOUND)
        val newAssignee = userScopeRepository.findByScopeTypeAndScopeIdAndUsernameIgnoreCase(
            username, ScopeType.ORGANIZATION, deadline.organization.id
        ).orElseThrow{ StatusCodeException(400, ErrorCode.INVITATION_NOT_ORG_MEMBER) }
        requirePermission(
            permissionService.canManageAssignees(issuer, DeadlineScope(deadline))
        )

        userScopeRepository.save(
            UserScope(
                0,
                newAssignee.user,
                ScopeType.DEADLINE,
                deadline.id,
                role,
                Instant.now()
            )
        )
    }

    @Transactional
    fun removeAssignee(issuer: User, deadlineId: Long, assigneeUsername: String) {
        if (assigneeUsername.equals(issuer.username, ignoreCase = true)) {
            throw StatusCodeException(400, ErrorCode.ACTION_SELF_REMOVAL)
        }

        requirePermission(
            permissionService.canManageAssignees(issuer, DeadlineScope(
                deadlineRepository.findByIdOr404(deadlineId, ErrorCode.DDL_NOT_FOUND)
            ))
        )

        val userToRemove = userRepository.findByUsernameIgnoreCaseOr404(assigneeUsername)
        userScopeRepository.deleteByUserAndScopeId(userToRemove, null, null, deadlineId)
    }

    fun getDeadline(issuer: User, deadlineId: Long): DeadlineResponse {
        val deadline = deadlineRepository.findByIdOr404(deadlineId, ErrorCode.DDL_NOT_FOUND)

        requirePermission(
            permissionService.hasAccess(issuer, DeadlineScope(deadline))
        )

        val stats = deadlineRepository.getDeadlineStats(listOf(deadline.id))[0]
        return deadline.toResponse(
            stats,
            permissionService.buildDeadlinePermissions(issuer, deadline)
        )
    }

    private fun prepareDeadlineResponseData(user: User, deadlines: List<Deadline>, prefetchRoles: Boolean = true): Map<Long, DeadlineStatsDTO> {
        val deadlineIds = mutableSetOf<Long>()
        val threadIds = mutableSetOf<Long>()
        val organizationIds = mutableSetOf<Long>()

        for (deadline in deadlines) {
            deadlineIds.add(deadline.id)
            threadIds.add(deadline.thread.id)
            organizationIds.add(deadline.organization.id)
        }

        val deadlineIdsList = deadlineIds.toList()
        if (prefetchRoles) {
            permissionService.prefetchUserRoles(
                user,
                orgIds = organizationIds.toList(),
                thrIds = threadIds.toList(),
                ddlIds = deadlineIdsList
            )
        }

        return deadlineRepository.getDeadlineStats(deadlineIdsList)
            .associateBy { it.deadlineId }
    }

    private fun mapDeadlineToFullResponse(user: User, deadline: Deadline, stats: Map<Long, DeadlineStatsDTO>) =
        deadline.toResponse(stats[deadline.id]!!, permissionService.buildDeadlinePermissions(user, deadline)).withRole(
            permissionService.getRole(deadline.id, ScopeType.DEADLINE),
            permissionService.getMaxRole(
                listOf(
                    PermissionContext.PermissionKey(ScopeType.DEADLINE, deadline.id),
                    PermissionContext.PermissionKey(ScopeType.THREAD, deadline.thread.id),
                    PermissionContext.PermissionKey(ScopeType.ORGANIZATION, deadline.thread.organization.id)
                )
            ).takeIf {
                // TODO: PermissionService might be useful
                // The goal is to not return a "read only" role
                    maxRole -> maxRole > ScopeRole.DDL_ASSIGNEE
            }
        )

    fun getDeadlinesByUser(
        issuer: User,
        pageNumber: Int,
        pageSize: Int
    ): PaginationResponse<DeadlineResponseWithRole> {
        val userDeadlines = deadlineRepository.findAllByUser(
            issuer.id, PageRequest.of(pageNumber, pageSize)
        )

        val stats = prepareDeadlineResponseData(issuer, userDeadlines.toList())
        return PaginationResponse.fromPage(
            userDeadlines.map {
                mapDeadlineToFullResponse(issuer, it, stats)
            }
        )
    }

    fun getDeadlinesByThread(
        issuer: User,
        threadId: Long,
        pageNumber: Int,
        pageSize: Int
    ): PaginationResponse<DeadlineResponseWithRole> {
        val thread = threadRepository.findByIdOr404(threadId, ErrorCode.THR_NOT_FOUND)

        requirePermission(
            permissionService.hasAccess(issuer, ThreadScope(thread))
        )

        val threadDeadlines = deadlineRepository.findAllByThread(
            thread, PageRequest.of(pageNumber, pageSize)
        )
        val stats = prepareDeadlineResponseData(issuer, threadDeadlines.toList())
        return PaginationResponse.fromPage(
            threadDeadlines.map {
                mapDeadlineToFullResponse(issuer, it, stats)
            }
        )
    }

    @Transactional
    fun patchDeadline(
        issuer: User,
        deadlineId: Long,
        title: String?,
        description: String?,
        isCompleted: Boolean?,
        due: Instant?
    ) {
        if (
            title == null && description == null &&
            isCompleted == null && due == null
        ) {
            return
        }

        val deadline = deadlineRepository.findByIdOr404(deadlineId, ErrorCode.DDL_NOT_FOUND)
        requirePermission(
            permissionService.canUpdate(issuer, DeadlineScope(deadline))
        )

        if (due != null) {
            if (due.isBefore(Instant.now())) {
                throw StatusCodeException(400, ErrorCode.DDL_INVALID_TIMESTAMP)
            }
            val timeShiftSeconds = Duration.between(deadline.due, due).toSeconds()
            deadline.due = due
            deadlineNotificationRepository.updateSendAtAndResetStatusByDeadline(
                deadline.id, timeShiftSeconds
            )
        }


        if (title != null) deadline.title = title
        if (description != null) deadline.description = description
        if (isCompleted != null) deadline.isCompleted = isCompleted

        deadlineRepository.save(deadline)
    }

    fun getDeadlineAssignees(
        issuer: User,
        deadlineId: Long,
        pageNumber: Int,
        pageSize: Int
    ): PaginationResponse<UserScopeResponse> {
        val deadline = deadlineRepository.findByIdOr404(deadlineId, ErrorCode.DDL_NOT_FOUND)

        requirePermission(
            permissionService.hasAccess(issuer, DeadlineScope(deadline))
        )

        val pageRequest = PageRequest.of(pageNumber, pageSize, Sort.by("role").descending())
        return userScopeRepository.findAllByScopeIdAndScopeType(
            deadlineId, ScopeType.DEADLINE, pageRequest
        ).toPaginationResponse { it.toResponse() }
    }
}