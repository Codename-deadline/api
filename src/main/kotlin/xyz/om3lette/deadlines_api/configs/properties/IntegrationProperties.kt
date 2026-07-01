package xyz.om3lette.deadlines_api.configs.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("integration")
data class IntegrationProperties(
    val telegram: Telegram = Telegram()
) {
    data class Telegram(
        val botToken: String = ""
    )
}
