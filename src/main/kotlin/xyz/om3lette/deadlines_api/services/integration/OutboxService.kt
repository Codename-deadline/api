package xyz.om3lette.deadlines_api.services.integration

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service
import xyz.om3lette.deadlines_api.data.notifications.enums.NotificationStatus
import xyz.om3lette.deadlines_api.data.outbox.enums.ProcessResult
import xyz.om3lette.deadlines_api.data.outbox.model.Outbox
import xyz.om3lette.deadlines_api.data.outbox.repo.OutboxRepository
import xyz.om3lette.deadlines_api.services.integration.outboxHandler.OutboxHandler


@Service
class OutboxService(
    private val outboxRepository: OutboxRepository,
    handlers: List<OutboxHandler>
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    @Value("\${outbox.batch-size}") private val batchSize = 200
    @Value("\${outbox.max-retries}") private val maxRetries = 5

    private val handlersByTopic: Map<String, OutboxHandler> =
        handlers
            .groupBy { it.topic }
            .mapValues { (k, v) ->
                require(v.size == 1) { "Multiple handlers for topic $k" }
                v[0]
            }

    /**
     * Claim a batch, process them and update DB
     * Meant to be called periodically (scheduled) from a worker pool
     */
    fun pollOnce() {
        val claimed = outboxRepository.claimBatch(batchSize, maxRetries)
        if (claimed.isEmpty()) return

        val successIds = mutableListOf<Long>()
        val toSave = mutableListOf<Outbox>()

        claimed.forEach { outbox ->
            val handler = handlersByTopic[outbox.topic]
            if (handler == null) {
                logger.error("Handler not found for outbox: $outbox")
                return@forEach
            }

            val result = try {
                handler.handle(outbox.messenger, outbox.payload)
            } catch(_: Exception) {
                ProcessResult.RETRY
            }

            when (result) {
                ProcessResult.SUCCESS -> successIds.add(outbox.id)
                ProcessResult.RETRY -> {
                    outbox.status =
                        if (outbox.retries < maxRetries) NotificationStatus.PENDING
                        else NotificationStatus.FAILED
                    toSave += outbox
                }
                ProcessResult.PERMANENT_FAILURE -> {
                    outbox.status = NotificationStatus.FAILED
                    toSave += outbox
                }
            }
        }

        if (successIds.isNotEmpty()) {
            outboxRepository.deleteAllByIdInBatch(successIds)
        }

        if (toSave.isNotEmpty()) {
            outboxRepository.saveAllAndFlush(toSave)
        }
    }
}