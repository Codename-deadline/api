package xyz.om3lette.deadlines_api.services.auth.otp.otpSendHandlers

import xyz.om3lette.deadlines_api.data.integration.bot.enums.Language
import xyz.om3lette.deadlines_api.data.otp.event.OtpEvent
import xyz.om3lette.deadlines_api.redisData.otp.enums.OtpChannel

interface OtpSender {
    val channel: OtpChannel

    fun send(identifier: String?, code: String, language: Language)
}