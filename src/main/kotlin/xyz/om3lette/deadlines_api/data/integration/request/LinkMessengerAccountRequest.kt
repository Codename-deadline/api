package xyz.om3lette.deadlines_api.data.integration.request

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Messenger

data class LinkMessengerAccountRequest(
    @field:Positive
    val accountId: Long,

    @field:NotNull
    val messenger: Messenger
)
