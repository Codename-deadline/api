package xyz.om3lette.deadlines_api.configs.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("users")
data class UsersProperties(
    val maxSessions: Int = 50,
    val maxLinkedAccountsPerMessenger: Int = 2
)
