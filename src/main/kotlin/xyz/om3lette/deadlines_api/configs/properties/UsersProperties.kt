package xyz.om3lette.deadlines_api.configs.properties

import jakarta.validation.constraints.Min
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties("users")
data class UsersProperties(
    @field:Min(1)
    val maxSessions: Int = 50,

    @field:Min(1)
    val maxLinkedAccountsPerMessenger: Int = 1
)
