package xyz.om3lette.deadlines_api.services.auth.otp

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Language
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Messenger
import xyz.om3lette.deadlines_api.data.integration.messengerAccount.repo.UserMessengerAccountRepository
import xyz.om3lette.deadlines_api.data.jwt.dto.TokenPair
import xyz.om3lette.deadlines_api.data.otp.response.OtpResponse
import xyz.om3lette.deadlines_api.data.otp.response.OtpSignInResponse
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.exceptions.type.StatusCodeException
import xyz.om3lette.deadlines_api.redisData.otp.enums.OtpChannel
import xyz.om3lette.deadlines_api.redisData.otp.model.Otp
import xyz.om3lette.deadlines_api.redisData.otp.model.OtpPasswordCheck
import xyz.om3lette.deadlines_api.redisData.otp.model.OtpRegisterRequest
import xyz.om3lette.deadlines_api.redisData.otp.repo.OtpPasswordCheckRepository
import xyz.om3lette.deadlines_api.redisData.otp.repo.OtpRegisterRequestRepository
import xyz.om3lette.deadlines_api.redisData.otp.repo.OtpRepository
import xyz.om3lette.deadlines_api.services.auth.AuthService
import xyz.om3lette.deadlines_api.services.auth.otp.otpSendHandlers.OtpSender
import xyz.om3lette.deadlines_api.util.generateNumericCode
import java.util.UUID

@Service
class OtpService(
    private val authService: AuthService,
    private val authenticationManager: AuthenticationManager,
    private val userMessengerAccountRepository: UserMessengerAccountRepository,
    private val passwordEncoder: PasswordEncoder,
    private val otpRepository: OtpRepository,
    private val otpRegisterRequestRepository: OtpRegisterRequestRepository,
    private val otpPasswordCheckRepository: OtpPasswordCheckRepository,
    otpSenders: List<OtpSender>
) {

    private val topicToOtpSender = otpSenders
        .groupBy { it.channel }
        .mapValues { (k, v) ->
            require(v.size == 1) { "Multiple handlers for channel $k" }
            v[0]
        }

    private val logger = LoggerFactory.getLogger(OtpService::class.java)

    private val maxOtpAttempts: Int = 3

    @PostConstruct
    private fun validateSenders() {
        require(topicToOtpSender.keys.size == OtpChannel.entries.size) {
            logger.error("Number of senders does not match the number of available channels")
            "Number of senders does not match the number of available channels"
        }
    }

    fun createRegisterRequest(
        identifier: String,
        channel: OtpChannel,
        username: String,
        fullName: String,
        language: Language?,
    ): OtpResponse {
        val registerRequestId = otpRegisterRequestRepository.save(
            OtpRegisterRequest(
                username = username,
                fullName = fullName,
                language = language,
                identifier = identifier,
                channel = channel
            )
        ).id
        return OtpResponse(
            createAndSendOtp(identifier, channel, language, registerRequestId)
        )
    }

    fun createAndSendOtpForUser(
        identifier: String,
        channel: OtpChannel,
        username: String
    ): OtpResponse {
//      TODO: Check for channel not being a messenger
        val accountId: Long = identifier.toLong()
        val messenger = Messenger.valueOf(channel.name)

        val linkedAccount = userMessengerAccountRepository.findAccountByUsernameAndMessengerAndAccountId(
            username, messenger, accountId
        ) ?: throw StatusCodeException(404, "No linked account or user found")
        val otpId = createAndSendOtp(identifier, channel, linkedAccount.user.language, username = username)

        return OtpResponse(otpId)
    }


    // TODO: Consider using something lighter than bcrypt
    private fun createAndSendOtp(
        identifier: String,
        channel: OtpChannel,
        language: Language? = null,
        registerRequestId: UUID? = null,
        username: String? = null
    ): UUID {
        val code = generateNumericCode(6)
        val hashedCode = passwordEncoder.encode(code)

        val otp = otpRepository.save(
            Otp(
                hashedCode = hashedCode,
                registerRequestId = registerRequestId,
                username = username
            )
        )
        sendOtp(identifier.trim(), channel, code, language ?: Language.EN)

        return otp.id
    }

    fun signInOtp(otpId: UUID, code: String): OtpSignInResponse {
        val auth = authenticationManager.authenticate(OtpAuthenticationToken(otpId, code))
        val user = auth.principal as User
//      TODO: Consider creating enum for authority
        if (auth.authorities.all { it.authority != "OTP_VERIFIED" }) {
            return OtpSignInResponse.OK(authService.signInNoPasswordCheck(user))
        }
        val requestId = otpPasswordCheckRepository.save(
            OtpPasswordCheck(username = user.username)
        ).id
        return OtpSignInResponse.PasswordRequired(requestId)
    }

    fun completePassword(requestId: UUID, password: String): TokenPair {
        val request: OtpPasswordCheck = otpPasswordCheckRepository.findById(requestId).orElseThrow {
//          Imitate authenticationManager error
            BadCredentialsException("")
        }

        return try {
            authService.signInPassword(request.username, password)
        } catch (e: Exception) {
            request.attempts++
            if (request.attempts >= maxOtpAttempts) {
                otpPasswordCheckRepository.delete(request)
            }
            throw e
        }
    }

    private fun sendOtp(identifier: String?, channel: OtpChannel, code: String, language: Language) {
        val sender: OtpSender? = topicToOtpSender[channel]
        if (sender == null) {
            logger.error("No sender found for channel ${channel.name}")
            throw StatusCodeException(500, "Error occurred")
        }
        sender.send(identifier, code, language)
    }
}