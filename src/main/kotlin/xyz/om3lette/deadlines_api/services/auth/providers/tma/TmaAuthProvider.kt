package xyz.om3lette.deadlines_api.services.auth.providers.tma

import io.github.sanvew.tg.init.data.InitDataUtils.isValid
import io.github.sanvew.tg.init.data.InitDataUtils.parse
import io.github.sanvew.tg.init.data.type.InitData
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Language
import xyz.om3lette.deadlines_api.exceptions.type.StatusCodeException
import xyz.om3lette.deadlines_api.redisData.otp.enums.OtpChannel
import xyz.om3lette.deadlines_api.services.auth.AuthService
import xyz.om3lette.deadlines_api.services.auth.otp.UserProvisioningService

@Service
class TmaAuthProvider(
    private val authService: AuthService,
    private val userProvisioningService: UserProvisioningService
) {
    @Value("\${integration.telegram.bot-token}")
    private lateinit var botToken: String

    fun register(
        initData: String,
        preferredUsername: String?
    ): AuthService.TokenPair {
        if (!isValid(initData, botToken)) throw StatusCodeException(403, "Invalid credentials")
        val data: InitData = parse(initData)

        val userData = data.user
        val username = preferredUsername ?: userData?.username
        if (userData == null || username == null) {
            throw StatusCodeException(400, "No user data provided")
        }

        val fullName = userData.firstName + (userData.lastName ?: "")
        val language = try {
            Language.valueOf(userData.languageCode?.uppercase() ?: "EN")
        } catch (_: Exception) {
            Language.EN
        }

        val user = userProvisioningService.registerUserFromOtpRequest(
            userData.username!!,
            fullName,
            OtpChannel.TELEGRAM,
            language,
            userData.id.toString()
        )
        return authService.signInNoPasswordCheck(user)
    }
}