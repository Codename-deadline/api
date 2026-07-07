package xyz.om3lette.deadlines_api.configs.properties

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties("spring.redis")
data class RedisProperties(
    @field:NotBlank
    val hostname: String = "localhost",

    @field:Min(1)
    @field:Max(65535)
    val port: Int = 6379
)
