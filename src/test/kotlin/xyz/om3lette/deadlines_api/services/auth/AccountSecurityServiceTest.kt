package xyz.om3lette.deadlines_api.services.auth

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.security.crypto.password.PasswordEncoder
import xyz.om3lette.deadlines_api.DomainObjectBuilder
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.data.user.repo.UserRepository
import xyz.om3lette.deadlines_api.exceptions.type.StatusCodeException
import kotlin.test.assertEquals

@ExtendWith(MockKExtension::class)
class AccountSecurityServiceTest {
    private val passwordEncoder: PasswordEncoder = mockk()
    private val userRepository: UserRepository = mockk()
    private val authSessionService: AuthSessionService = mockk()
    private val accountSecurityService = AccountSecurityService(
        passwordEncoder,
        userRepository,
        authSessionService
    )

    private lateinit var user: User

    @BeforeEach
    fun commonStubs() {
        user = DomainObjectBuilder.user(username = "bob", password = "current-user-password")
        every { passwordEncoder.encode("new-password") } returns "encoded-new-password"
        every { passwordEncoder.matches("old-password", "current-user-password") } returns true
        every { passwordEncoder.matches("wrong-password", "current-user-password") } returns false
    }

    @Test
    fun `same old and new password throws 400`() {
        val ex = assertThrows<StatusCodeException> {
            accountSecurityService.changePassword(user, "same-password", "same-password")
        }

        assertEquals(400, ex.statusCode)
    }

    @Test
    fun `wrong old password throws 403`() {
        val ex = assertThrows<StatusCodeException> {
            accountSecurityService.changePassword(user, "wrong-password", "new-password")
        }

        assertEquals(403, ex.statusCode)
    }

    @Test
    fun `user without existing password can set password without old password`() {
        val userWithoutPassword = DomainObjectBuilder.user(password = null)
        val savedUser = slot<User>()
        every { userRepository.save(capture(savedUser)) } returnsArgument 0
        every { authSessionService.revokeAllSessions(userWithoutPassword) } returns Unit

        accountSecurityService.changePassword(userWithoutPassword, null, "new-password")

        assertEquals("encoded-new-password", savedUser.captured.password)
    }

    @Test
    fun `happy path updates password and revokes sessions`() {
        val savedUser = slot<User>()
        every { userRepository.save(capture(savedUser)) } returnsArgument 0
        every { authSessionService.revokeAllSessions(user) } returns Unit

        accountSecurityService.changePassword(user, "old-password", "new-password")

        assertAll(
            { assertEquals(user.id, savedUser.captured.id) },
            { assertEquals("encoded-new-password", savedUser.captured.password) },
            { verify(exactly = 1) { authSessionService.revokeAllSessions(user) } }
        )
    }
}
