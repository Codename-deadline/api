package xyz.om3lette.deadlines_api.configs.properties

import jakarta.validation.constraints.Min
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties("outbox")
data class OutboxProperties(
    @field:Min(1)
    val batchSize: Int = 200,

    @field:Min(0)
    val maxRetries: Int = 5
)
