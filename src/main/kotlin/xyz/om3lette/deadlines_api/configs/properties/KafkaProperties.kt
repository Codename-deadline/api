package xyz.om3lette.deadlines_api.configs.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("spring.kafka")
data class KafkaProperties(
    val bootstrapServers: String = "localhost:9092"
)
