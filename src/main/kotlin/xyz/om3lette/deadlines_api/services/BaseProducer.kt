package xyz.om3lette.deadlines_api.services

import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Messenger

open class BaseProducer<T : Any>(
    private val topic: String,
    private val accountLinkageKafkaTemplate: KafkaTemplate<String, T>,
) {
    private val logger = LoggerFactory.getLogger(BaseProducer::class.java)

    fun sendToMessenger(messenger: Messenger, event: T) {
        val key = messenger.toString()
        accountLinkageKafkaTemplate.send(topic, key, event)
            .thenAccept {
                logger.info("Sent {} to {}", event::class.simpleName, messenger.name)
            }
            .exceptionally { ex ->
                logger.error("Failed to send {} to {}: {}", event::class.simpleName, messenger.name, ex.message)
                null
            }
    }
}
