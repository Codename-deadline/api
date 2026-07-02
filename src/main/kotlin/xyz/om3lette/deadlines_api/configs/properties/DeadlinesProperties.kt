package xyz.om3lette.deadlines_api.configs.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("deadlines")
data class DeadlinesProperties(
    val maxAssignees: Long = 10
)
