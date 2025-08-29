package xyz.om3lette.deadlines_api.tasks

import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import xyz.om3lette.deadlines_api.data.notifications.repo.DeadlineNotificationRepository


@Service
@EnableAsync
class NotificationPublishingTask(
    private val deadlineNotificationRepository: DeadlineNotificationRepository
) {
    @Value("\${outbox.batch-size}")private val batchSize = 200

    @Async
    @Scheduled(fixedRate = 60 * 1000)
    fun run() =
        deadlineNotificationRepository.findNotificationRecipientsAndInsertIntoOutbox(batchSize)
}