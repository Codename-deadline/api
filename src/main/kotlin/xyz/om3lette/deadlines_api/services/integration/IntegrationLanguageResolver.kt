package xyz.om3lette.deadlines_api.services.integration

import org.springframework.stereotype.Component
import xyz.om3lette.deadlines_api.configs.properties.IntegrationProperties
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Language
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Messenger
import xyz.om3lette.deadlines_api.data.integration.chat.model.Chat
import xyz.om3lette.deadlines_api.data.integration.messengerAccount.repo.UserMessengerAccountRepository
import kotlin.jvm.optionals.getOrNull

@Component
class IntegrationLanguageResolver(
    private val userMessengerAccountRepository: UserMessengerAccountRepository,
    private val integrationProperties: IntegrationProperties,
) {
    val fallbackLanguage: Language
        get() = integrationProperties.fallbackLanguage

    fun resolve(messenger: Messenger?, accountId: Long?): Language {
        if (messenger == null || accountId == null) return fallbackLanguage

        return userMessengerAccountRepository.findByMessengerAndAccountId(messenger, accountId)
            .getOrNull()?.user?.language
            ?: fallbackLanguage
    }

    fun resolve(chat: Chat?, messenger: Messenger?, accountId: Long?): Language =
        chat?.language ?: resolve(messenger, accountId)
}
