package xyz.om3lette.deadlines_api.services.auth.otp

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Test
import xyz.om3lette.deadlines_api.db.constraintViolation
import xyz.om3lette.deadlines_api.data.common.constraints.DatabaseConstraint
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Language
import xyz.om3lette.deadlines_api.data.integration.messengerAccount.model.UserMessengerAccount
import xyz.om3lette.deadlines_api.data.integration.messengerAccount.repo.UserMessengerAccountRepository
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.data.user.repo.UserRepository
import xyz.om3lette.deadlines_api.exceptions.enums.ErrorCode
import xyz.om3lette.deadlines_api.exceptions.type.StatusCodeException
import xyz.om3lette.deadlines_api.redisData.otp.enums.OtpChannel
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UserRegistrationServiceTest {
    private val userRepository: UserRepository = mockk()
    private val messengerAccountRepository: UserMessengerAccountRepository = mockk()
    private val service = UserRegistrationService(userRepository, messengerAccountRepository)

    @Test
    fun `registration flushes both records`() {
        val savedUser = slot<User>()
        val savedAccount = slot<UserMessengerAccount>()
        every { userRepository.saveAndFlush(capture(savedUser)) } answers { savedUser.captured }
        every { messengerAccountRepository.saveAndFlush(capture(savedAccount)) } answers { savedAccount.captured }

        val result = service.registerExternalUser(
            "  username  ",
            "  Full Name  ",
            OtpChannel.TELEGRAM,
            Language.RU,
            "1234"
        )

        assertEquals("username", result.username)
        assertEquals("Full Name", result.fullName)
        assertEquals(Language.RU, result.language)
        assertEquals(1234, savedAccount.captured.accountId)
        assertEquals(result, savedAccount.captured.user)
    }

    @Test
    fun `duplicate username is mapped after flush`() {
        every { userRepository.saveAndFlush(any()) } throws
            constraintViolation(DatabaseConstraint.UQ_USERS_USERNAME_LOWER)

        val error = assertFailsWith<StatusCodeException> {
            service.registerExternalUser(
                "username",
                "Full Name",
                OtpChannel.TELEGRAM,
                Language.EN,
                "1234"
            )
        }

        assertEquals(ErrorCode.USER_ALREADY_EXISTS, error.code)
    }

    @Test
    fun `duplicate messenger account is mapped after flush`() {
        every { userRepository.saveAndFlush(any()) } answers { firstArg() }
        every { messengerAccountRepository.saveAndFlush(any()) } throws
            constraintViolation(DatabaseConstraint.UQ_USER_MESSENGER_ACCOUNTS_ACCOUNT_MESSENGER)

        val error = assertFailsWith<StatusCodeException> {
            service.registerExternalUser(
                "username",
                "Full Name",
                OtpChannel.TELEGRAM,
                Language.EN,
                "1234"
            )
        }

        assertEquals(ErrorCode.INTEGRATION_ACCOUNT_ALREADY_IN_USE, error.code)
    }
}
