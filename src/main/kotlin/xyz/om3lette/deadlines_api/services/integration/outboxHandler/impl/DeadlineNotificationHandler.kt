package xyz.om3lette.deadlines_api.services.integration.outboxHandler.impl

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Messenger
import xyz.om3lette.deadlines_api.data.notifications.event.DeadlineNotificationEvent
import xyz.om3lette.deadlines_api.services.integration.kafka.NotificationProducer
import xyz.om3lette.deadlines_api.services.integration.outboxHandler.OutboxHandler
import xyz.om3lette.deadlines_api.data.outbox.enums.ProcessResult


@Component
class DeadlineNotificationHandler(
    private val notificationProducer: NotificationProducer
) : OutboxHandler {
    override val topic: String = "private.integration.notifications"

    private val logger = LoggerFactory.getLogger(DeadlineNotificationHandler::class.java)

    private val mapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)


    override fun handle(messenger: Messenger, payload: JsonNode): ProcessResult {
        return try {
            notificationProducer.sendToMessenger(
                messenger,
                mapper.treeToValue(payload, DeadlineNotificationEvent::class.java)
            )
            ProcessResult.SUCCESS
        } catch (e: Exception) {
            logger.error("Failed to send deadline notification: $e")
            ProcessResult.RETRY
        }
    }
}