package xyz.om3lette.deadlines_api.data.integration.messengerAccount.dto

import xyz.om3lette.deadlines_api.data.integration.bot.enums.Messenger

data class MessengerAccountDTO(
    val accountId: Long,
    val messenger: Messenger
)
