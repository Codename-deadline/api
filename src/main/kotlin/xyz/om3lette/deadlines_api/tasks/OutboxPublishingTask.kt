package xyz.om3lette.deadlines_api.tasks

import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import xyz.om3lette.deadlines_api.services.integration.OutboxService

@Service
@EnableAsync
class OutboxPublishingTask(
    private val outboxService: OutboxService
) {
    @Async
    @Scheduled(fixedRate = 60 * 1000)
    fun run() = outboxService.pollOnce()
}