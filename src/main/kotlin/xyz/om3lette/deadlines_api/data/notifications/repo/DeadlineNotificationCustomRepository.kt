package xyz.om3lette.deadlines_api.data.notifications.repo

interface DeadlineNotificationCustomRepository {
    fun findNotificationRecipientsAndInsertIntoOutbox(batchSize: Int)
    fun finalizeProcessedNotifications(notificationIds: Collection<Long>)
}
