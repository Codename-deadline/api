package xyz.om3lette.deadlines_api.data.integration.bot.dto

import xyz.om3lette.deadlines_api.data.integration.bot.enums.Messenger

data class BotDTO(
    val botId: Long,
    val username: String,
    val messenger: Messenger
)
