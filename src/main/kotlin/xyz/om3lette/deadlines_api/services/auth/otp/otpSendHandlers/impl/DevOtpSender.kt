package xyz.om3lette.deadlines_api.services.auth.otp.otpSendHandlers.impl

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Language
import xyz.om3lette.deadlines_api.redisData.otp.enums.OtpChannel
import xyz.om3lette.deadlines_api.services.auth.otp.otpSendHandlers.OtpSender

@Service
@Profile("dev")
class DevOtpSender : OtpSender {
    override val channel = OtpChannel.TELEGRAM

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun send(identifier: String?, code: String, language: Language) {
        logger.info("Issued an otp: $code")
    }
}
