package xyz.om3lette.deadlines_api.services.integration

import org.springframework.stereotype.Component
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Language
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Messenger
import xyz.om3lette.deadlines_api.data.integration.chat.model.Chat
import xyz.om3lette.deadlines_api.data.integration.messengerAccount.repo.UserMessengerAccountRepository
import kotlin.jvm.optionals.getOrNull

@Component
class IntegrationLanguageResolver(
    private val userMessengerAccountRepository: UserMessengerAccountRepository
) {
    fun resolve(messenger: Messenger?, accountId: Long?): Language {
        if (messenger == null || accountId == null) return Language.EN

        return userMessengerAccountRepository.findByMessengerAndAccountId(messenger, accountId)
            .getOrNull()?.user?.language
            ?: Language.EN
    }

    fun resolve(chat: Chat?, messenger: Messenger?, accountId: Long?): Language =
        chat?.language ?: resolve(messenger, accountId)
}
