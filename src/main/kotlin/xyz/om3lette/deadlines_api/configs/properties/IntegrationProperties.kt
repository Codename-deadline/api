package xyz.om3lette.deadlines_api.configs.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Language

@ConfigurationProperties("integration")
data class IntegrationProperties(
    val telegram: Telegram = Telegram(),
    val fallbackLanguage: Language = Language.EN,
) {
    data class Telegram(
        val botToken: String = ""
    )
}
