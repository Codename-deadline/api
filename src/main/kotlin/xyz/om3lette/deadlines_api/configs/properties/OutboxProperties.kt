package xyz.om3lette.deadlines_api.configs.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("outbox")
data class OutboxProperties(
    val batchSize: Int = 200,
    val maxRetries: Int = 5
)
