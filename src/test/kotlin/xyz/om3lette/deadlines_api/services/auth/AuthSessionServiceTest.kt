package xyz.om3lette.deadlines_api.services.auth

import io.jsonwebtoken.Claims
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
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
import xyz.om3lette.deadlines_api.DomainObjectBuilder
import xyz.om3lette.deadlines_api.configs.properties.UsersProperties
import xyz.om3lette.deadlines_api.data.jwt.model.RefreshToken
import xyz.om3lette.deadlines_api.data.jwt.repo.RefreshTokenRepository
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.data.user.repo.UserRepository
import xyz.om3lette.deadlines_api.exceptions.enums.ErrorCode
import xyz.om3lette.deadlines_api.exceptions.type.StatusCodeException
import xyz.om3lette.deadlines_api.services.JwtService
import java.time.Instant
import java.util.Date
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExtendWith(MockKExtension::class)
class AuthSessionServiceTest {
    private val jwtService: JwtService = mockk()
    private val userRepository: UserRepository = mockk()
    private val refreshTokenRepository: RefreshTokenRepository = mockk()
    private val maxSessions = 2
    private val authSessionService = AuthSessionService(
        UsersProperties(maxSessions = maxSessions),
        jwtService,
        userRepository,
        refreshTokenRepository
    )

    private lateinit var user: User

    @BeforeEach
    fun commonFixtures() {
        user = DomainObjectBuilder.user(id = 42, username = "bob")
    }

    private fun stubTokenGeneration(
        accessToken: String = "access-token",
        refreshToken: String = "refresh-token",
        refreshJti: String = "refresh-jti"
) {
        every { jwtService.generateAccessToken(user) } returns Pair(accessToken, "access-jti")
        every { jwtService.generateRefreshToken(user) } returns Pair(refreshToken, refreshJti)
        every { jwtService.extractExpiration(refreshToken) } returns Date.from(Instant.now().plusSeconds(60))
    }

    @Nested
    inner class IssueSession {
        @Test
        fun `session limit reached throws 400`() {
            every { refreshTokenRepository.findAllValidByUser(user) } returns listOf(
                DomainObjectBuilder.refreshToken(user, id = 1),
                DomainObjectBuilder.refreshToken(user, id = 2)
            )

            val ex = assertThrows<StatusCodeException> { authSessionService.issueSession(user) }

            assertAll(
                { assertEquals(400, ex.statusCode) },
                { assertEquals(ErrorCode.AUTH_SESSIONS_LIMIT_EXCEEDED, ex.code) }
            )
        }

        @Test
        fun `happy path returns token pair and persists refresh token`() {
            val savedRefreshToken = slot<RefreshToken>()
            every { refreshTokenRepository.findAllValidByUser(user) } returns emptyList()
            every { refreshTokenRepository.save(capture(savedRefreshToken)) } returnsArgument 0
            stubTokenGeneration(accessToken = "access", refreshToken = "refresh", refreshJti = "jti")

            val result = authSessionService.issueSession(user)

            assertAll(
                { assertEquals("access", result.accessToken) },
                { assertEquals("refresh", result.refreshToken) },
                { assertEquals("jti", savedRefreshToken.captured.jti) },
                { assertFalse(savedRefreshToken.captured.revoked) },
                { assertEquals(user, savedRefreshToken.captured.user) }
            )
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class RefreshSession {
        private val jwt = "valid.jwt"
        private lateinit var claims: Claims
        private lateinit var existingRefreshToken: RefreshToken

        @BeforeEach
        fun commonHappyStubs() {
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

            val ex = assertThrows<StatusCodeException> { authSessionService.refreshSession(jwt) }

            assertAll(
                { assertEquals(401, ex.statusCode) },
                { assertEquals(ErrorCode.AUTH_INVALID_CREDENTIALS, ex.code) }
            )
        }

        @ParameterizedTest
        @MethodSource("badClaimsProvider")
        fun `missing subject or jti throws 401`(subject: String?, jti: String?) = assertInvalidCredentials {
            every { claims.subject } returns subject
            every { claims["jti"] } returns jti
        }

        @Test
        fun `invalid jwt throws 401`() = assertInvalidCredentials {
            every { jwtService.extractAllClaims(jwt) } throws IllegalArgumentException()
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
            val savedRefreshToken = slot<RefreshToken>()
            every { refreshTokenRepository.save(existingRefreshToken) } returns existingRefreshToken
            every { refreshTokenRepository.save(capture(savedRefreshToken)) } returnsArgument 0
            stubTokenGeneration(accessToken = "new-access", refreshToken = "new-refresh", refreshJti = "new-jti")

            val result = authSessionService.refreshSession(jwt)

            assertAll(
                { assertTrue(existingRefreshToken.revoked) },
                { verify { refreshTokenRepository.save(existingRefreshToken) } },
                { assertEquals("new-access", result.accessToken) },
                { assertEquals("new-refresh", result.refreshToken) },
                { assertEquals("new-jti", savedRefreshToken.captured.jti) }
            )
        }
    }

    @Test
    fun `revokeAllSessions revokes and saves all valid tokens`() {
        val validTokens = listOf(
            DomainObjectBuilder.refreshToken(user, id = 1),
            DomainObjectBuilder.refreshToken(user, id = 2)
        )
        val savedTokens = slot<List<RefreshToken>>()
        every { refreshTokenRepository.findAllValidByUser(user) } returns validTokens
        every { refreshTokenRepository.saveAll(capture(savedTokens)) } returnsArgument 0

        authSessionService.revokeAllSessions(user)

        assertTrue(savedTokens.captured.all { it.revoked })
    }
}
