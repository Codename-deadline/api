package xyz.om3lette.deadlines_api.data.otp.event

import xyz.om3lette.deadlines_api.data.integration.bot.enums.Language

data class OtpEvent(
    val code: String,

    val accountId: Long,

    val language: Language
)