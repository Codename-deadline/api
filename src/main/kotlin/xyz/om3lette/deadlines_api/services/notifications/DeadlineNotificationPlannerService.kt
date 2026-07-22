package xyz.om3lette.deadlines_api.services.notifications

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import xyz.om3lette.deadlines_api.data.notifications.enums.NotificationStatus
import xyz.om3lette.deadlines_api.data.notifications.enums.TimeRemaining
import xyz.om3lette.deadlines_api.data.notifications.model.DeadlineNotification
import xyz.om3lette.deadlines_api.data.notifications.repo.DeadlineNotificationRepository
import xyz.om3lette.deadlines_api.data.scopes.deadline.model.Deadline
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class DeadlineNotificationPlannerService(
    private val deadlineNotificationRepository: DeadlineNotificationRepository
) {
    private data class PlannedNotification(
        val type: TimeRemaining,
        val sendAt: Instant
    )

    private val reminderOffsets = listOf(
        TimeRemaining.FIFTEEN_MINUTES to Duration.ofMinutes(15),
        TimeRemaining.ONE_HOUR to Duration.ofHours(1),
        TimeRemaining.ONE_DAY to Duration.ofDays(1),
        TimeRemaining.ONE_WEEK to Duration.ofDays(7),
        TimeRemaining.ONE_MONTH to Duration.ofSeconds(31556952L / 12),
        TimeRemaining.NO_TIME to Duration.ZERO
    )

    fun createNotifications(deadline: Deadline, now: Instant = Instant.now()) {
        deadlineNotificationRepository.saveAll(
            planNotifications(deadline.due, now).map {
                DeadlineNotification(
                    id = 0,
                    deadline = deadline,
                    sendAt = it.sendAt,
                    type = it.type,
                    status = NotificationStatus.PENDING
                )
            }
        )
    }

    @Transactional
    fun reconcileNotifications(deadline: Deadline, now: Instant = Instant.now()) {
        val plannedByType = planNotifications(deadline.due, now).associateBy { it.type }
        val pendingByType = deadlineNotificationRepository
            .findAllByDeadlineAndStatus(deadline, NotificationStatus.PENDING)
            .associateBy { it.type }

        val obsoletePending = mutableListOf<DeadlineNotification>()
        val updatedPending = mutableListOf<DeadlineNotification>()
        for (pendingNotification in pendingByType.values) {
            val plannedNotification = plannedByType[pendingNotification.type]
            if (plannedNotification == null) {
                obsoletePending.add(pendingNotification)
            } else {
                pendingNotification.sendAt = plannedNotification.sendAt
                updatedPending.add(pendingNotification)
            }
        }

        val newNotifications = plannedByType.values
            .filter { pendingByType[it.type] == null }
            .map {
                DeadlineNotification(
                    id = 0,
                    deadline = deadline,
                    sendAt = it.sendAt,
                    type = it.type,
                    status = NotificationStatus.PENDING
                )
            }

        if (obsoletePending.isNotEmpty()) {
            deadlineNotificationRepository.deleteAll(obsoletePending)
        }
        if (updatedPending.isNotEmpty()) {
            deadlineNotificationRepository.saveAll(updatedPending)
        }
        if (newNotifications.isNotEmpty()) {
            deadlineNotificationRepository.saveAll(newNotifications)
        }
    }

    @Transactional
    fun deleteNotifications(deadline: Deadline) {
        deadlineNotificationRepository.deleteAllByDeadline(deadline)
    }

    private fun planNotifications(due: Instant, now: Instant): List<PlannedNotification> = reminderOffsets
        .map { (type, offset) ->
            PlannedNotification(type, due.minus(offset).truncatedTo(ChronoUnit.MINUTES))
        }
        .filter { it.sendAt.isAfter(now) }
}
