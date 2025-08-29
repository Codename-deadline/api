package xyz.om3lette.deadlines_api.data.notifications.repo

import jakarta.transaction.Transactional
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import xyz.om3lette.deadlines_api.data.notifications.model.DeadlineNotification
import xyz.om3lette.deadlines_api.data.scopes.deadline.model.Deadline
import java.time.Instant
// TODO Update the status if new sendAt is >= now()
interface DeadlineNotificationRepository : JpaRepository<DeadlineNotification, Long>, DeadlineNotificationCustomRepository {
    @Transactional
    @Modifying
    @Query(
        value = """
            UPDATE deadline_notifications
            SET send_at = send_at + MAKE_INTERVAL(secs => :deltaSeconds), status = 'P'
            WHERE deadline_id = :deadlineId
        """,
        nativeQuery = true
    )
    fun updateSendAtAndResetStatusByDeadline(deadlineId: Long, deltaSeconds: Long): Int
}