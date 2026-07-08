package xyz.om3lette.deadlines_api.data.integration.common.dto

import xyz.om3lette.deadlines_api.data.integration.bot.enums.Language
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Messenger
import xyz.om3lette.deadlines_api.data.integration.messengerAccount.model.UserMessengerAccount
import xyz.om3lette.deadlines_api.data.user.model.User

data class IssuerContext(
    val messenger: Messenger,
    val accountId: Long,
    val messengerAccount: UserMessengerAccount,
) {
    val user: User
        get() = messengerAccount.user

    val language: Language
        get() = user.language
}
