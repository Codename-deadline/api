package xyz.om3lette.deadlines_api.data.notifications.event

import xyz.om3lette.deadlines_api.data.integration.bot.enums.Language
import xyz.om3lette.deadlines_api.data.notifications.enums.TimeRemaining
import java.time.Instant

data class RawDeadline(
    val id: Long,
    val due: Instant,
    val title: String
)

data class DeadlineNotificationEvent(
    val chatId: Long,
    val language: Language,
    val deadline: RawDeadline,
    val timeRemaining: TimeRemaining
)
