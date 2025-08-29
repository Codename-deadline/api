package xyz.om3lette.deadlines_api.data.otp.request

import xyz.om3lette.deadlines_api.data.integration.bot.enums.Language
import xyz.om3lette.deadlines_api.redisData.otp.enums.OtpChannel

data class RegisterOtpRequest(
    val identifier: String,

    val channel: OtpChannel,

    val username: String,

    val fullName: String,

    val language: Language?
)
