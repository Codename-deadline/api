package xyz.om3lette.deadlines_api.configs.properties

import jakarta.validation.constraints.NotEmpty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties("cors")
data class CorsProperties(
    @field:NotEmpty
    val allowedOrigins: List<String> = listOf("http://localhost:5173"),

    @field:NotEmpty
    val allowedMethods: List<String> = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"),

    @field:NotEmpty
    val allowedHeaders: List<String> = listOf("*")
)
