package xyz.om3lette.deadlines_api.data.notifications.repo

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import xyz.om3lette.deadlines_api.data.notifications.enums.NotificationStatus
import xyz.om3lette.deadlines_api.data.notifications.model.DeadlineNotification
import xyz.om3lette.deadlines_api.data.scopes.deadline.model.Deadline

interface DeadlineNotificationRepository : JpaRepository<DeadlineNotification, Long>, DeadlineNotificationCustomRepository {
    // Used to fetch the notifications which sendAt can be updated
    // To avoid them being sent in the background before the row is updated
    // a LockModeType.PESSIMISTIC_WRITE lock is used
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findAllByDeadlineAndStatus(
        deadline: Deadline,
        status: NotificationStatus
    ): List<DeadlineNotification>
}
