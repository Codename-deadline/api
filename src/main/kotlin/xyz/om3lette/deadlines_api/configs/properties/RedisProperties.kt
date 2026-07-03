package xyz.om3lette.deadlines_api.configs.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("spring.redis")
data class RedisProperties(
    val hostname: String = "localhost",
    val port: Int = 6379
)
