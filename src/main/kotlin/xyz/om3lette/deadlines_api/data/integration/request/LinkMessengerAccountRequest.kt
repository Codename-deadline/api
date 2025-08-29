package xyz.om3lette.deadlines_api.data.integration.request

import xyz.om3lette.deadlines_api.data.integration.bot.enums.Messenger

data class LinkMessengerAccountRequest(
    val accountId: Long,
    val messenger: Messenger
)