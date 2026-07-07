package xyz.om3lette.deadlines_api.configs.properties

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties("spring.security.jwt")
data class JwtProperties(
    @field:NotBlank
    @field:Size(min=64)
    val secret: String,

    @field:Min(1)
    val accessExpiration: Long = 10_000,

    @field:Min(1)
    val refreshExpiration: Long = 604_800
)
