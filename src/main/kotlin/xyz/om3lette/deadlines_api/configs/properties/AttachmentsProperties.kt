package xyz.om3lette.deadlines_api.configs.properties

import jakarta.validation.constraints.Min
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties("attachments")
data class AttachmentsProperties(
    @field:Min(0)
    val maxPerDeadline: Long = 25
)
