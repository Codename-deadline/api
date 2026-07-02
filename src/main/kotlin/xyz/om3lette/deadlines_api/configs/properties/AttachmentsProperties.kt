package xyz.om3lette.deadlines_api.configs.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("attachments")
data class AttachmentsProperties(
    val maxPerDeadline: Long = 25
)
