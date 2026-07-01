package xyz.om3lette.deadlines_api.configs.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("spring.security.jwt")
data class JwtProperties(
    val secret: String,
    val accessExpiration: Long = 10_000,
    val refreshExpiration: Long = 604_800
)
