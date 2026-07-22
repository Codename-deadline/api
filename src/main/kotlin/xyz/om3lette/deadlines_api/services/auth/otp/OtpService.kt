package xyz.om3lette.deadlines_api.services.auth.otp

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.stereotype.Service
import xyz.om3lette.deadlines_api.configs.properties.OtpProperties
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Language
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Messenger
import xyz.om3lette.deadlines_api.data.integration.messengerAccount.repo.UserMessengerAccountRepository
import xyz.om3lette.deadlines_api.data.jwt.dto.TokenPair
import xyz.om3lette.deadlines_api.data.otp.constraints.OtpConstraints
import xyz.om3lette.deadlines_api.data.otp.response.OtpResponse
import xyz.om3lette.deadlines_api.data.otp.response.OtpSignInResponse
import xyz.om3lette.deadlines_api.data.user.repo.UserRepository
import xyz.om3lette.deadlines_api.exceptions.enums.ErrorCode
import xyz.om3lette.deadlines_api.exceptions.type.StatusCodeException
import xyz.om3lette.deadlines_api.redisData.otp.enums.OtpChanelType
import xyz.om3lette.deadlines_api.redisData.otp.enums.OtpChannel
import xyz.om3lette.deadlines_api.redisData.otp.enums.OtpPurpose
import xyz.om3lette.deadlines_api.redisData.otp.model.Otp
import xyz.om3lette.deadlines_api.redisData.otp.model.OtpPasswordCheck
import xyz.om3lette.deadlines_api.redisData.otp.model.OtpRegisterRequest
import xyz.om3lette.deadlines_api.redisData.otp.repo.OtpPasswordCheckRepository
import xyz.om3lette.deadlines_api.redisData.otp.repo.OtpRegisterRequestRepository
import xyz.om3lette.deadlines_api.redisData.otp.repo.OtpRepository
import xyz.om3lette.deadlines_api.services.auth.AuthSessionService
import xyz.om3lette.deadlines_api.services.auth.PasswordAuthService
import xyz.om3lette.deadlines_api.services.auth.otp.otpSendHandlers.OtpSender
import xyz.om3lette.deadlines_api.util.generateNumericCode
import java.util.*

@Service
class OtpService(
    private val authSessionService: AuthSessionService,
    private val passwordAuthService: PasswordAuthService,
    private val userMessengerAccountRepository: UserMessengerAccountRepository,
    private val otpCodeHasher: OtpCodeHasher,
    private val otpVerificationService: OtpVerificationService,
    private val userRegistrationService: UserRegistrationService,
    private val userRepository: UserRepository,
    private val otpRepository: OtpRepository,
    private val otpRegisterRequestRepository: OtpRegisterRequestRepository,
    private val otpPasswordCheckRepository: OtpPasswordCheckRepository,
    private val otpProperties: OtpProperties,
    otpSenders: List<OtpSender>
) {

    private val topicToOtpSender = otpSenders
        .groupBy { it.channel }
        .mapValues { (k, v) ->
            require(v.size == 1) { "Multiple handlers for channel $k" }
            v[0]
        }

    private val logger = LoggerFactory.getLogger(OtpService::class.java)

    @PostConstruct
    private fun validateSenders() {
        require(topicToOtpSender.keys.size == OtpChannel.entries.size) {
            "Number of senders does not match the number of available channels"
        }
    }

    fun sendRegisterOtpRequest(
        identifier: String,
        channel: OtpChannel,
        username: String,
        fullName: String,
        language: Language?,
    ): OtpResponse {
        if (
            channel.type == OtpChanelType.MESSENGER
            && userMessengerAccountRepository.existsByAccountId(identifier.toLong())
        ) {
            throw StatusCodeException(409, ErrorCode.INTEGRATION_ACCOUNT_ALREADY_IN_USE)
        }

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
            createAndSendOtp(
                identifier,
                channel,
                language,
                OtpPurpose.REGISTRATION,
                registerRequestId.toString()
            )
        )
    }

    fun sendSignInOtpRequest(
        identifier: String,
        channel: OtpChannel,
        username: String
    ): OtpResponse {
        val accountId: Long = try {
            identifier.toLong()
        } catch (_: NumberFormatException) {
            throw StatusCodeException(422, ErrorCode.INTEGRATION_INVALID_IDENTIFIER_FORMAT)
        }

        val messenger = when(channel.type) {
            OtpChanelType.MESSENGER -> Messenger.valueOf(channel.name)
        }

        val linkedAccount = userMessengerAccountRepository.findAccountByUsernameAndMessengerAndAccountId(
            username, messenger, accountId
        ) ?: throw StatusCodeException(404, ErrorCode.INTEGRATION_ACCOUNT_NOT_LINKED)
        val otpId = createAndSendOtp(
            identifier.trim(),
            channel,
            linkedAccount.user.language,
            OtpPurpose.SIGN_IN,
            context = linkedAccount.user.username
        )

        return OtpResponse(otpId)
    }

    private fun createAndSendOtp(
        identifier: String,
        channel: OtpChannel,
        language: Language? = null,
        purpose: OtpPurpose,
        context: String
    ): UUID {
        val code = generateNumericCode(OtpConstraints.CODE_LENGTH)
        val hashedCode: String = otpCodeHasher.hash(code)

        val otp = otpRepository.save(
            Otp(
                hashedCode = hashedCode,
                purpose = purpose,
                context = context
            )
        )
        sendOtp(identifier.trim(), channel, code, language ?: Language.EN)

        return otp.id
    }

    fun verifyOtpAndFulfillRequest(otpId: UUID, code: String): OtpSignInResponse {
        val verifiedOtp = otpVerificationService.verifyAndConsume(otpId, code)
        return when (verifiedOtp.purpose) {
            OtpPurpose.REGISTRATION -> completeRegistrationOtp(verifiedOtp.context)
            OtpPurpose.SIGN_IN -> completeSignInOtp(verifiedOtp.context)
        }
    }

    private fun completeRegistrationOtp(registerRequestIdContext: String): OtpSignInResponse {
        val registerRequestId = try {
            UUID.fromString(registerRequestIdContext)
        } catch (_: IllegalArgumentException) {
            throw StatusCodeException(404, ErrorCode.SIGN_UP_REGISTRATION_REQUEST_NOT_FOUND)
        }
        val registerRequest = otpRegisterRequestRepository.findById(registerRequestId).orElseThrow {
            StatusCodeException(404, ErrorCode.SIGN_UP_REGISTRATION_REQUEST_NOT_FOUND)
        }

        return try {
            val user = userRegistrationService.registerExternalUser(
                registerRequest.username,
                registerRequest.fullName,
                registerRequest.channel,
                registerRequest.language,
                registerRequest.identifier
            )
            OtpSignInResponse.OK(authSessionService.issueSession(user))
        } finally {
            otpRegisterRequestRepository.deleteById(registerRequestId)
        }
    }

    private fun completeSignInOtp(username: String): OtpSignInResponse {
        val user = userRepository.findByUsernameIgnoreCase(username).orElseThrow {
            BadCredentialsException("")
        }

        if (user.password.isNullOrBlank()) {
            return OtpSignInResponse.OK(authSessionService.issueSession(user))
        }
        val requestId = otpPasswordCheckRepository.save(
            OtpPasswordCheck(username = user.username)
        ).id
        return OtpSignInResponse.PasswordRequired(requestId)
    }

    fun completePassword(requestId: UUID, password: String): TokenPair {
        val request: OtpPasswordCheck = otpPasswordCheckRepository.findById(requestId).orElseThrow {
            // Imitate authenticationManager error
            BadCredentialsException("")
        }

        return try {
            passwordAuthService.signIn(request.username, password)
        } catch (e: Exception) {
            request.attempts++
            if (request.attempts >= otpProperties.maxAttempts) {
                otpPasswordCheckRepository.delete(request)
            } else {
                otpPasswordCheckRepository.save(request)
            }
            throw e
        }
    }

    private fun sendOtp(identifier: String?, channel: OtpChannel, code: String, language: Language) {
        val sender: OtpSender? = topicToOtpSender[channel]
        if (sender == null) {
            logger.error("No sender found for channel ${channel.name}")
            throw StatusCodeException(500, ErrorCode.UNKNOWN_ERROR)
        }
        sender.send(identifier, code, language)
    }
}
