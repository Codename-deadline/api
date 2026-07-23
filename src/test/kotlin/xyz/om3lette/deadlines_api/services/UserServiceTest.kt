package xyz.om3lette.deadlines_api.services

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import xyz.om3lette.deadlines_api.DomainObjectBuilder
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Language
import xyz.om3lette.deadlines_api.data.integration.messengerAccount.repo.UserMessengerAccountRepository
import xyz.om3lette.deadlines_api.data.user.repo.UserRepository
import xyz.om3lette.deadlines_api.exceptions.enums.ErrorCode
import xyz.om3lette.deadlines_api.exceptions.type.StatusCodeException
import kotlin.test.assertEquals

class UserServiceTest {
    private val userRepository: UserRepository = mockk()
    private val messengerAccountRepository: UserMessengerAccountRepository = mockk()
    private val userService = UserService(userRepository, messengerAccountRepository)

    @Test
    fun `patch user updates and persists profile`() {
        val user = DomainObjectBuilder.user(username = "before", fullName = "Before Name")
        every { userRepository.existsByUsernameIgnoreCase("after") } returns false
        every { userRepository.save(user) } returns user

        userService.patchUser(user, "after", "After Name", Language.RU, "Asia/Tokyo")

        assertEquals("after", user.username)
        assertEquals("After Name", user.fullName)
        assertEquals(Language.RU, user.language)
        assertEquals("Asia/Tokyo", user.timeZone)
        verify(exactly = 1) { userRepository.save(user) }
    }

    @Test
    fun `patch user rejects another user's username`() {
        val user = DomainObjectBuilder.user(username = "before")
        every { userRepository.existsByUsernameIgnoreCase("taken") } returns true

        val error = assertThrows<StatusCodeException> {
            userService.patchUser(user, "taken", user.fullName, user.language, null)
        }

        assertEquals(409, error.statusCode)
        assertEquals(ErrorCode.USER_ALREADY_EXISTS, error.code)
        verify(exactly = 0) { userRepository.save(any()) }
    }
}
