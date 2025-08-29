package xyz.om3lette.deadlines_api.services.auth.otp

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Language
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Messenger
import xyz.om3lette.deadlines_api.data.integration.messengerAccount.model.UserMessengerAccount
import xyz.om3lette.deadlines_api.data.integration.messengerAccount.repo.UserMessengerAccountRepository
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.data.user.repo.UserRepository
import xyz.om3lette.deadlines_api.exceptions.type.StatusCodeException
import xyz.om3lette.deadlines_api.redisData.otp.enums.OtpChannel
import xyz.om3lette.deadlines_api.redisData.otp.repo.OtpRegisterRequestRepository
import java.time.Instant
import java.util.UUID

// Separating concern of creating a user from OtpRegistrationRequest and
// Avoiding circular dependency
// ┌─────┐
// |  securityConfig
// ↑     ↓
// |  otpAuthProvider
// ↑     ↓
// |  otpService
// ↑     ↓
// |  authService
// └─────┘

@Service
class UserProvisioningService(
    private val userRepository: UserRepository,
    private val userMessengerAccountRepository: UserMessengerAccountRepository,
    private val otpRegisterRequestRepository: OtpRegisterRequestRepository
) {
    @Transactional
    fun registerUserFromPending(pendingId: UUID): String {
        val userData = otpRegisterRequestRepository.findById(pendingId).orElseThrow {
            StatusCodeException(404, "Registration data not found")
        }
        return registerUserFromOtpRequest(
            userData.username,
            userData.fullName,
            userData.channel,
            userData.language,
            userData.identifier
        ).username
    }

    @Transactional
    fun registerUserFromOtpRequest(
        username: String,
        fullName: String,
        channel: OtpChannel,
        language: Language?,
        identifier: String
    ): User {
        val user = try {
            userRepository.save(
                User(
                    _username = username.trim(),
                    joinedAt = Instant.now(),
                    fullName = fullName.trim(),
                    language = language ?: Language.EN
                )
            )
        } catch (_: DataIntegrityViolationException) {
            throw StatusCodeException(409, "User already exists")
        }
        // TODO: Check for channel not being a messenger
        val messenger = Messenger.valueOf(channel.name)
        try {
            userMessengerAccountRepository.save(
                UserMessengerAccount(
                    0, user, identifier.toLong(), messenger
                )
            )
        } catch (_: DataIntegrityViolationException) {
            throw StatusCodeException(409, "Account already in use")
        }
        return user
    }
}