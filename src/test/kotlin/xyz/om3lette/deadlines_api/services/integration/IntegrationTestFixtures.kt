package xyz.om3lette.deadlines_api.services.integration

import xyz.om3lette.deadlines_api.DomainObjectBuilder
import xyz.om3lette.deadlines_api.configs.properties.IntegrationProperties
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Language
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Messenger
import xyz.om3lette.deadlines_api.data.integration.bot.model.Bot
import xyz.om3lette.deadlines_api.data.integration.chat.model.Chat
import xyz.om3lette.deadlines_api.data.integration.messengerAccount.model.UserMessengerAccount
import xyz.om3lette.deadlines_api.data.user.enums.UserRole
import xyz.om3lette.deadlines_api.data.user.model.User
import java.time.Instant

object IntegrationTestFixtures {
    const val ISSUER_ACCOUNT_ID = 12_345L
    const val MESSENGER_CHAT_ID = -100_000L
    const val BOT_ID = 42L

    fun user(
        id: Long = 1,
        language: Language = Language.EN,
        role: UserRole = UserRole.USER,
    ): User = DomainObjectBuilder.user(id = id, language = language, role = role)

    fun admin(language: Language = Language.EN): User =
        user(id = 2, language = language, role = UserRole.ADMIN)

    fun messengerAccount(
        user: User = user(),
        id: Long = 10,
        accountId: Long = ISSUER_ACCOUNT_ID,
        messenger: Messenger = Messenger.TELEGRAM,
    ) = UserMessengerAccount(id, user, accountId, messenger)

    fun bot(
        id: Long = 20,
        botId: Long = BOT_ID,
        messenger: Messenger = Messenger.TELEGRAM,
    ) = Bot(id, messenger, botId, "deadlines_bot", mutableListOf())

    fun chat(
        id: Long = 30,
        messengerChatId: Long = MESSENGER_CHAT_ID,
        messenger: Messenger = Messenger.TELEGRAM,
        title: String = "Deadlines chat",
        language: Language = Language.EN,
        timeZone: String = "Etc/UTC",
        bot: Bot = bot(),
    ) = Chat(id, messengerChatId, messenger, title, bot, language, timeZone, Instant.parse("2026-07-08T07:00:00Z"))

    fun integrationProperties(fallbackLanguage: Language = Language.EN) =
        IntegrationProperties(fallbackLanguage = fallbackLanguage)
}
