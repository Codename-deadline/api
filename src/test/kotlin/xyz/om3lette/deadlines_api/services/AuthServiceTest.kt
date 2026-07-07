package xyz.om3lette.deadlines_api.services

import io.jsonwebtoken.Claims
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.password.PasswordEncoder
import xyz.om3lette.deadlines_api.DomainObjectBuilder
import xyz.om3lette.deadlines_api.configs.properties.UsersProperties
import xyz.om3lette.deadlines_api.data.integration.bot.enums.Language
import xyz.om3lette.deadlines_api.data.jwt.model.RefreshToken
import xyz.om3lette.deadlines_api.data.jwt.repo.RefreshTokenRepository
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.data.user.repo.UserRepository
import xyz.om3lette.deadlines_api.exceptions.enums.ErrorCode
import xyz.om3lette.deadlines_api.exceptions.type.StatusCodeException
import xyz.om3lette.deadlines_api.services.auth.AuthService
import java.time.Instant
import java.util.Date
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExtendWith(MockKExtension::class)
class AuthServiceTest {
    private val maxSessions = 2

    private val jwtService: JwtService = mockk()
    private val authManager: AuthenticationManager = mockk()
    private val passwordEncoder: PasswordEncoder = mockk()
    private val userRepository: UserRepository = mockk()
    private val refreshTokenRepository: RefreshTokenRepository = mockk()

    private val service = AuthService(
        UsersProperties(maxSessions = maxSessions),
        jwtService,
        authManager,
        passwordEncoder,
        userRepository,
        refreshTokenRepository
    )

    private lateinit var user: User

    @BeforeEach
    fun commonFixtures() {
        user = DomainObjectBuilder.user(
            id = 42,
            username = "bob",
            fullName = "Bob the tester",
            password = "current-user-password"
        )
    }

    private fun stubAuthenticatedUser(username: String = user.username, password: String = "raw-password") {
        val auth = mockk<Authentication> {
            every { principal } returns user
        }
        every {
            authManager.authenticate(
                match<UsernamePasswordAuthenticationToken> {
                    it.principal == username && it.credentials == password
                }
            )
        } returns auth
    }

    private fun stubGeneratedTokenPair(
        accessToken: String = "access-jwt",
        refreshToken: String = "refresh-jwt",
        refreshJti: String = "refresh-jti",
        savedRefreshTokenSlot: CapturingSlot<RefreshToken> = slot()
    ): CapturingSlot<RefreshToken> {
        every { jwtService.generateAccessToken(user) } returns Pair(accessToken, "access-jti")
        every { jwtService.generateRefreshToken(user) } returns Pair(refreshToken, refreshJti)
        every { jwtService.extractExpiration(refreshToken) } returns Date.from(Instant.now().plusSeconds(60))
        every { refreshTokenRepository.save(capture(savedRefreshTokenSlot)) } returnsArgument 0
        return savedRefreshTokenSlot
    }

    @Nested
    inner class Register {
        @BeforeEach
        fun commonHappyStubs() {
            every { passwordEncoder.encode("strong-password") } returns "encoded-password"
            every { userRepository.save(any()) } returnsArgument 0
        }

        @Test
        fun `duplicate username throws 409`() {
            every { userRepository.save(any()) } throws DataIntegrityViolationException("")

            val ex = assertThrows<StatusCodeException> {
                service.registerWithPassword("bob", "Bob the tester", "strong-password", null)
            }

            assertEquals(409, ex.statusCode)
        }

        @Test
        fun `happy path creates user with encoded password and default language`() {
            val savedUser = slot<User>()
            every { userRepository.save(capture(savedUser)) } returnsArgument 0

            service.registerWithPassword("bob", "Bob the tester", "strong-password", null)

            assertAll(
                { assertEquals("bob", savedUser.captured.username) },
                { assertEquals("Bob the tester", savedUser.captured.fullName) },
                { assertEquals("encoded-password", savedUser.captured.password) },
                { assertEquals(Language.EN, savedUser.captured.language) }
            )
        }
    }

    @Nested
    inner class SignIn {
        @Test
        fun `session limit reached throws 400`() {
            stubAuthenticatedUser()
            every { refreshTokenRepository.findAllValidByUser(user) } returns listOf(
                DomainObjectBuilder.refreshToken(user, id = 1),
                DomainObjectBuilder.refreshToken(user, id = 2)
            )

            val ex = assertThrows<StatusCodeException> {
                service.signInPassword(user.username, "raw-password")
            }

            assertAll(
                { assertEquals(400, ex.statusCode) },
                { assertEquals(ErrorCode.AUTH_SESSIONS_LIMIT_EXCEEDED, ex.code) }
            )
        }

        @Test
        fun `happy path returns token pair and persists refresh token`() {
            stubAuthenticatedUser()
            every { refreshTokenRepository.findAllValidByUser(user) } returns emptyList()
            val savedRefreshToken = stubGeneratedTokenPair(
                accessToken = "access-token",
                refreshToken = "refresh-token",
                refreshJti = "refresh-jti"
            )

            val result = service.signInPassword("bob", "raw-password")

            assertAll(
                { assertEquals("access-token", result.accessToken) },
                { assertEquals("refresh-token", result.refreshToken) },
                { assertEquals("refresh-jti", savedRefreshToken.captured.jti) },
                { assertFalse(savedRefreshToken.captured.revoked) },
                { assertEquals(user, savedRefreshToken.captured.user) }
            )
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class TokenRefresh {
        private val jwt = "valid.jwt"
        private lateinit var request: HttpServletRequest
        private lateinit var claims: Claims
        private lateinit var existingRefreshToken: RefreshToken

        @BeforeEach
        fun commonHappyStubs() {
            request = mockk {
                every { getHeader("Authorization") } returns "Bearer $jwt"
            }
            claims = mockk {
                every { subject } returns user.username
                every { this@mockk["jti"] } returns "refresh-jti"
            }
            existingRefreshToken = DomainObjectBuilder.refreshToken(user, jti = "refresh-jti")

            every { jwtService.extractAllClaims(jwt) } returns claims
            every { userRepository.findByUsernameIgnoreCase(user.username) } returns Optional.of(user)
            every { refreshTokenRepository.findByJti("refresh-jti") } returns Optional.of(existingRefreshToken)
        }

        fun badClaimsProvider() = listOf(
            Arguments.of(null, "refresh-jti"),
            Arguments.of("bob", null),
            Arguments.of(null, null)
        )

        private fun assertInvalidCredentials(stub: () -> Unit) {
            stub()

            val ex = assertThrows<StatusCodeException> { service.refreshToken(request) }

            assertAll(
                { assertEquals(401, ex.statusCode) },
                { assertEquals(ErrorCode.AUTH_INVALID_CREDENTIALS, ex.code) }
            )
        }

        @Test
        fun `missing Authorization header throws 401`() = assertInvalidCredentials {
            request = mockk {
                every { getHeader("Authorization") } returns null
            }
        }

        @Test
        fun `invalid Authorization header format throws 401`() = assertInvalidCredentials {
            request = mockk {
                every { getHeader("Authorization") } returns "Bearer-token"
            }
        }

        @ParameterizedTest
        @MethodSource("badClaimsProvider")
        fun `missing subject or jti throws 401`(subject: String?, jti: String?) = assertInvalidCredentials {
            every { claims.subject } returns subject
            every { claims["jti"] } returns jti
        }

        @Test
        fun `user not found throws 401`() = assertInvalidCredentials {
            every { userRepository.findByUsernameIgnoreCase(user.username) } returns Optional.empty()
        }

        @Test
        fun `refresh token not found throws 401`() = assertInvalidCredentials {
            every { refreshTokenRepository.findByJti("refresh-jti") } returns Optional.empty()
        }

        @Test
        fun `revoked refresh token throws 401`() = assertInvalidCredentials {
            existingRefreshToken.revoked = true
        }

        @Test
        fun `happy path revokes old token and returns new token pair`() {
            val savedRefreshToken = stubGeneratedTokenPair(
                accessToken = "new-access",
                refreshToken = "new-refresh",
                refreshJti = "new-refresh-jti"
            )

            val result = service.refreshToken(request)

            assertAll(
                { assertTrue(existingRefreshToken.revoked) },
                { verify { refreshTokenRepository.save(existingRefreshToken) } },
                { assertEquals("new-access", result.accessToken) },
                { assertEquals("new-refresh", result.refreshToken) },
                { assertEquals("new-refresh-jti", savedRefreshToken.captured.jti) }
            )
        }
    }

    @Nested
    inner class ChangePassword {
        @BeforeEach
        fun commonHappyStubs() {
            every { passwordEncoder.encode("new-password") } returns "encoded-new-password"
            every { passwordEncoder.matches("old-password", "current-user-password") } returns true
            every { passwordEncoder.matches("wrong-password", "current-user-password") } returns false
        }

        @Test
        fun `same old and new password throws 400`() {
            val ex = assertThrows<StatusCodeException> {
                service.changePassword(user, "same-password", "same-password")
            }

            assertEquals(400, ex.statusCode)
        }

        @Test
        fun `wrong old password throws 403`() {
            val ex = assertThrows<StatusCodeException> {
                service.changePassword(user, "wrong-password", "new-password")
            }

            assertEquals(403, ex.statusCode)
        }

        @Test
        fun `user without existing password can set password without old password`() {
            val userWithoutPassword = DomainObjectBuilder.user(password = null)
            val savedUser = slot<User>()
            every { userRepository.save(capture(savedUser)) } returnsArgument 0
            every { refreshTokenRepository.findAllValidByUser(userWithoutPassword) } returns emptyList()
            every { refreshTokenRepository.saveAll(emptyList()) } returns emptyList()

            service.changePassword(userWithoutPassword, null, "new-password")

            assertEquals("encoded-new-password", savedUser.captured.password)
        }

        @Test
        fun `happy path updates password and revokes valid refresh tokens`() {
            val validTokens = listOf(
                DomainObjectBuilder.refreshToken(user, id = 1),
                DomainObjectBuilder.refreshToken(user, id = 2)
            )
            val savedUser = slot<User>()
            val savedTokens = slot<List<RefreshToken>>()
            every { userRepository.save(capture(savedUser)) } returnsArgument 0
            every { refreshTokenRepository.findAllValidByUser(user) } returns validTokens
            every { refreshTokenRepository.saveAll(capture(savedTokens)) } returnsArgument 0

            service.changePassword(user, "old-password", "new-password")

            assertAll(
                { assertEquals(user.id, savedUser.captured.id) },
                { assertEquals("encoded-new-password", savedUser.captured.password) },
                { assertTrue(savedTokens.captured.all { it.revoked }) },
                { assertEquals(validTokens.size, savedTokens.captured.size) }
            )
        }
    }

    @Nested
    inner class SignOut {
        @Test
        fun `happy path revokes all valid refresh tokens`() {
            val validTokens = listOf(
                DomainObjectBuilder.refreshToken(user, id = 1),
                DomainObjectBuilder.refreshToken(user, id = 2)
            )
            val savedTokens = slot<List<RefreshToken>>()
            every { refreshTokenRepository.findAllValidByUser(user) } returns validTokens
            every { refreshTokenRepository.saveAll(capture(savedTokens)) } returnsArgument 0

            service.signOut(user)

            assertTrue(savedTokens.captured.all { it.revoked })
        }
    }
}
