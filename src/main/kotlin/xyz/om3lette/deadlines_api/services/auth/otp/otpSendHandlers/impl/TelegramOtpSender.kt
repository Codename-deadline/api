package xyz.om3lette.deadlines_api.services.auth.otp.otpSendHandlers.impl

import org.springframework.stereotype.Service
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Language
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Messenger
import xyz.om3lette.deadlines_api.data.otp.event.OtpEvent
import xyz.om3lette.deadlines_api.exceptions.type.StatusCodeException
import xyz.om3lette.deadlines_api.redisData.otp.enums.OtpChannel
import xyz.om3lette.deadlines_api.services.auth.otp.kafka.OtpProducer
import xyz.om3lette.deadlines_api.services.auth.otp.otpSendHandlers.OtpSender

@Service
class TelegramOtpSender(
    private val otpProducer: OtpProducer
) : OtpSender {
    override val channel = OtpChannel.TELEGRAM


    override fun send(identifier: String?, code: String, language: Language) {
        val accountId = try {
            identifier?.toLong()
        } catch (_: NumberFormatException) {
           null
        } ?: throw StatusCodeException(400, "Invalid account id: $identifier")
        otpProducer.sendToMessenger(Messenger.TELEGRAM, OtpEvent(code, accountId, language))
    }
}
