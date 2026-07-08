package xyz.om3lette.deadlines_api.configs.properties

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties("spring.kafka")
data class KafkaProperties(
    @field:NotBlank
    val bootstrapServers: String = "localhost:9092"
)
