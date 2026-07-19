package xyz.om3lette.deadlines_api.configs.properties

import jakarta.validation.constraints.Min
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties("deadlines")
data class DeadlinesProperties(
    @field:Min(0)
    val maxAssignees: Long = 10,

    @field:Min(15)
    val minExpiryMinutes: Long = 15
)
