package xyz.om3lette.deadlines_api.configs.properties

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties("otp")
data class OtpProperties(
    @field:NotBlank
    val hashSecret: String = "dev-otp-hash-secret-change-me",

    @field:Min(1)
    val maxAttempts: Int = 3
)
