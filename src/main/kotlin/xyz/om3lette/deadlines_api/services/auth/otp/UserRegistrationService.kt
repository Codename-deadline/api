package xyz.om3lette.deadlines_api.services.auth.otp

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import xyz.om3lette.deadlines_api.data.common.constraints.DatabaseConstraint
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Language
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Messenger
import xyz.om3lette.deadlines_api.data.integration.messengerAccount.model.UserMessengerAccount
import xyz.om3lette.deadlines_api.data.integration.messengerAccount.repo.UserMessengerAccountRepository
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.data.user.repo.UserRepository
import xyz.om3lette.deadlines_api.exceptions.enums.ErrorCode
import xyz.om3lette.deadlines_api.exceptions.type.StatusCodeException
import xyz.om3lette.deadlines_api.redisData.otp.enums.OtpChanelType
import xyz.om3lette.deadlines_api.redisData.otp.enums.OtpChannel
import xyz.om3lette.deadlines_api.util.jpaRepository.violatesConstraint
import java.time.Instant

@Service
class UserRegistrationService(
    private val userRepository: UserRepository,
    private val userMessengerAccountRepository: UserMessengerAccountRepository
) {
    @Transactional
    fun registerExternalUser(
        username: String,
        fullName: String,
        channel: OtpChannel,
        language: Language?,
        identifier: String,
        timeZone: String
    ): User {
        val user = try {
            userRepository.saveAndFlush(
                User(
                    _username = username.trim(),
                    joinedAt = Instant.now(),
                    fullName = fullName.trim(),
                    language = language ?: Language.EN,
                    timeZone = timeZone
                )
            )
        } catch (error: DataIntegrityViolationException) {
            if (!error.violatesConstraint(DatabaseConstraint.UQ_USERS_USERNAME_LOWER)) throw error
            throw StatusCodeException(409, ErrorCode.USER_ALREADY_EXISTS)
        }

        val messenger = when (channel.type) {
            OtpChanelType.MESSENGER -> Messenger.valueOf(channel.name)
        }
        try {
            userMessengerAccountRepository.saveAndFlush(
                UserMessengerAccount(
                    0, user, identifier.toLong(), messenger
                )
            )
        } catch (error: DataIntegrityViolationException) {
            if (!error.violatesConstraint(DatabaseConstraint.UQ_USER_MESSENGER_ACCOUNTS_ACCOUNT_MESSENGER)) throw error
            throw StatusCodeException(409, ErrorCode.INTEGRATION_ACCOUNT_ALREADY_IN_USE)
        }
        return user
    }
}
