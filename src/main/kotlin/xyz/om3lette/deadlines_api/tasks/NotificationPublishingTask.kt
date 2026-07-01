package xyz.om3lette.deadlines_api.tasks

import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import xyz.om3lette.deadlines_api.configs.properties.OutboxProperties
import xyz.om3lette.deadlines_api.data.notifications.repo.DeadlineNotificationRepository


@Service
@EnableAsync
class NotificationPublishingTask(
    private val deadlineNotificationRepository: DeadlineNotificationRepository,
    outboxProperties: OutboxProperties
) {
    private val batchSize = outboxProperties.batchSize

    @Async
    @Scheduled(fixedRate = 60 * 1000)
    fun run() =
        deadlineNotificationRepository.findNotificationRecipientsAndInsertIntoOutbox(batchSize)
}
