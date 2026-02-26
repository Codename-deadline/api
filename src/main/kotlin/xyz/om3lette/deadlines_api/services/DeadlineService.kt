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
import xyz.om3lette.deadlines_api.data.scopes.deadline.model.Deadline
import xyz.om3lette.deadlines_api.data.scopes.deadline.repo.DeadlineRepository
import xyz.om3lette.deadlines_api.data.scopes.deadline.response.DeadlineCreatedResponse
import xyz.om3lette.deadlines_api.data.scopes.deadline.response.DeadlineResponse
import xyz.om3lette.deadlines_api.data.scopes.enums.ProgressionStatus
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
import xyz.om3lette.deadlines_api.services.permission.PermissionLookupService
import xyz.om3lette.deadlines_api.services.permission.PermissionService
import xyz.om3lette.deadlines_api.util.jpaRepository.findByIdOr404
import xyz.om3lette.deadlines_api.util.page.toPaginationResponse
import xyz.om3lette.deadlines_api.util.requirePermission
import xyz.om3lette.deadlines_api.util.user.isAdminOr
import xyz.om3lette.deadlines_api.util.userRepository.findByUsernameIgnoreCaseOr404
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@Service
class DeadlineService(
    private val minDeadlineExpiryTimeMinutes: Long = 15,
    private val userRepository: UserRepository,
    private val userScopeRepository: UserScopeRepository,
    private val threadRepository: ThreadRepository,
    private val deadlineRepository: DeadlineRepository,
    private val deadlineNotificationRepository: DeadlineNotificationRepository,
    private val permissionService: PermissionService,
    private val permissionLookupService: PermissionLookupService,
) {

    @Transactional
    fun createDeadline(
        issuer: User,
        threadId: Long,
        title: String,
        description: String?,
        due: Instant,
        currentStatus: ProgressionStatus,
        assigneesUsernames: List<String>
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
            permissionService.canCreateOrDeleteDeadline(issuer) {
                Optional.of(
                    userScopeRepository.findByUserAndScopeIdIn(
                        issuer,
                        listOf(threadId, thread.organization.id)
                    ).maxBy { it.role }
                )
            }
        )

        val deadline = deadlineRepository.save(
            Deadline(
                0,
                thread.organization, thread,
                title, description, 0, currentStatus,
                Instant.now(), due
            )
        )
        val deadlineAssigneeScopes: MutableList<UserScope> = mutableListOf()
        userScopeRepository.findByScopeIdInAndUsernameInIgnoreCase(
            thread.organization.id,
            assigneesUsernames.map { it.lowercase() }
        )
            .groupBy { it.user.id }.values
            .map{ scopes -> scopes.maxBy { it.role } }
            .forEach { userScope ->
                // If a user is an admin he has an access anyway => don't add another entry
                if (
                    userScope.user.isAdminOr {
                        userScope.role.isEqualOrHigherThan(ScopeRole.THR_ASSIGNEE)
                    }
                ) return@forEach
    
                deadlineAssigneeScopes.add(
                    UserScope(
                        0,
                        userScope.user,
                        ScopeType.DEADLINE,
                        deadline.id,
                        ScopeRole.DDL_ASSIGNEE,
                        Instant.now()
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
        return DeadlineCreatedResponse(deadline.id)
    }

    fun deleteDeadline(issuer: User, deadlineId: Long) {
        val (deadline, issuerScope) = permissionLookupService.getDeadlineAndHighestRoleUserScopeOr404(issuer, deadlineId)

        requirePermission(
            permissionService.canCreateOrDeleteDeadline(issuer, issuerScope)
        )

        deadlineRepository.delete(deadline)
    }

    @Transactional
    fun removeAssignee(issuer: User, deadlineId: Long, assigneeUsername: String) {
        if (assigneeUsername.equals(issuer.username, ignoreCase = true)) {
            throw StatusCodeException(400, ErrorCode.ACTION_SELF_REMOVAL)
        }
        val (_, issuerScope) = permissionLookupService.getDeadlineAndHighestRoleUserScopeOr404(issuer, deadlineId)

        requirePermission(
            permissionService.canManageDeadlineAssignees(issuer, issuerScope)
        )

        val userToRemove = userRepository.findByUsernameIgnoreCaseOr404(assigneeUsername)
        userScopeRepository.deleteByUserAndScopeId(userToRemove, deadlineId)
    }

    fun getDeadlineMetaData(issuer: User, deadlineId: Long): DeadlineResponse {
        val (deadline, issuerScope) = permissionLookupService.getDeadlineAndHighestRoleUserScopeOr404(issuer, deadlineId)

        requirePermission(
            permissionService.hasDeadlineAccess(issuer, issuerScope, deadline.organization)
        )

        return deadline.toResponse()
    }

    fun getDeadlinesByThread(
        issuer: User,
        threadId: Long,
        pageNumber: Int,
        pageSize: Int
    ): PaginationResponse<DeadlineResponse> {
        val (thread, issuerScope) = permissionLookupService.getThreadAndHighestRoleUserScopeOr404(issuer, threadId)

        requirePermission(
            permissionService.hasThreadAccess(issuer, issuerScope, thread.organization)
        )

        return deadlineRepository.findAllByThread(
            thread, PageRequest.of(pageNumber, pageSize)
        ).toPaginationResponse { it.toResponse() }
    }

    @Transactional
    fun patchDeadline(
        issuer: User,
        deadlineId: Long,
        title: String?,
        description: String?,
        progress: Short?,
        status: ProgressionStatus?,
        due: Instant?
    ) {
        if (
            title == null && description == null && progress == null &&
            status == null && due == null
        ) {
            return
        }

        val (deadline, issuerScope) = permissionLookupService.getDeadlineAndHighestRoleUserScopeOr404(issuer, deadlineId)
        requirePermission(
            permissionService.canUpdateDeadline(issuer, issuerScope)
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
        if (progress != null) deadline.progress = progress
        if (status != null) deadline.status = status

        deadlineRepository.save(deadline)
    }

    fun getDeadlineAssignees(
        issuer: User,
        deadlineId: Long,
        pageNumber: Int,
        pageSize: Int
    ): PaginationResponse<UserScopeResponse> {
        val (deadline, issuerScope) = permissionLookupService.getDeadlineAndHighestRoleUserScopeOr404(issuer, deadlineId)

        requirePermission(
            permissionService.hasDeadlineAccess(issuer, issuerScope, deadline.organization)
        )

        val pageRequest = PageRequest.of(pageNumber, pageSize, Sort.by("role").descending())
        return userScopeRepository.findAllByScopeId(
            deadlineId, pageRequest
        ).toPaginationResponse { it.toResponse() }
    }
}