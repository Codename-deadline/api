package xyz.om3lette.deadlines_api.data.integration.messengerAccount.dto

import jakarta.validation.constraints.Positive
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Messenger

data class UnlinkMessengerAccountRequest(
    @field:Positive
    val accountId: Long,
    val messenger: Messenger
)
