package xyz.om3lette.deadlines_api.services

import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Messenger
import java.util.concurrent.CompletableFuture

open class BaseProducer<T : Any>(
    private val topic: String,
    private val accountLinkageKafkaTemplate: KafkaTemplate<String, T>,
) {
    private val logger = LoggerFactory.getLogger(BaseProducer::class.java)

    fun sendToMessenger(
        messenger: Messenger,
        event: T,
    ): CompletableFuture<SendResult<String, T>> {
        val key = messenger.toString()
        return accountLinkageKafkaTemplate.send(topic, key, event)
            .whenComplete { _, error ->
                if (error != null) {
                    logger.error("Failed to send {} to {}: {}", event::class.simpleName, messenger.name, error.message)
                } else {
                    logger.info("Sent {} to {}", event::class.simpleName, messenger.name)
                }
            }
    }
}
