package xyz.om3lette.deadlines_api.services

import io.jsonwebtoken.Claims
import io.mockk.*
import io.mockk.junit5.MockKExtension
import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.password.PasswordEncoder
import xyz.om3lette.deadlines_api.data.jwt.model.RefreshToken
import xyz.om3lette.deadlines_api.data.jwt.repo.RefreshTokenRepository
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.data.user.repo.UserRepository
import xyz.om3lette.deadlines_api.exceptions.enums.ErrorCode
import xyz.om3lette.deadlines_api.exceptions.type.StatusCodeException
import xyz.om3lette.deadlines_api.services.auth.AuthService
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// TODO: Rewrite
@ExtendWith(MockKExtension::class)
class AuthServiceTest {
    private val maxSessions = 2

    private val jwtService: JwtService = mockk()
    private val authManager: AuthenticationManager = mockk()
    private val passwordEncoder: PasswordEncoder = mockk()
    private val userRepository: UserRepository = mockk()
    private val refreshTokenRepository: RefreshTokenRepository = mockk()

    private val service = AuthService(
        maxSessions,
        jwtService,
        authManager,
        passwordEncoder,
        userRepository,
        refreshTokenRepository
    )
    private val dummyUser = User(
        id = 42,
        _username = "bob",
        _password = "current-user-password",
        fullName = "Test User",
        joinedAt = Instant.now()
    )

    val dummyRefreshToken = RefreshToken(
        0,
        "someJti",
        Instant.now().plusSeconds(60),
        false,
        dummyUser
    )
    val dummyRefreshToken2 = RefreshToken(
        1,
        "someJti1",
        Instant.now().plusSeconds(60),
        false,
        dummyUser
    )
    val dummyTokens = listOf(dummyRefreshToken, dummyRefreshToken2)


    @Nested
    inner class Register {
        @BeforeEach
        fun commonHappyStubs() {
            every { passwordEncoder.encode("strong-password") } returns "encoded"
            every { userRepository.save(any()) } returnsArgument 0
        }

        @Test
        fun `when username exists, throws 409`() {
            every { userRepository.save(any()) } throws DataIntegrityViolationException("")
            val res = assertThrows<StatusCodeException> {
                service.registerWithPassword("Test1", "Bob the tester", "strong-password", null)
            }
            assertEquals(res.statusCode, 409)
        }

        @Test
        fun `happy path creates user`() {
            service.registerWithPassword("Bob", "Bob the tester", "strong-password", null)
            verify { userRepository.save(
                match { it.username == "Bob" && it.password?.startsWith("encoded") ?: false })
            }
        }
    }

    @Nested
    inner class SignIn {

        @Test
        fun `when sessions ge maxSessions, throws StatusCodeException`() {
            val auth = mockk<Authentication> {
                every { principal } returns dummyUser
            }
            every { authManager.authenticate(any()) } returns auth
            every { refreshTokenRepository.findAllValidByUser(dummyUser) } returns listOf(mockk(), mockk())
            val res = assertThrows<StatusCodeException> { service.signInPassword(dummyUser.username, "raw-pw") }

            assertEquals(res.statusCode, 400)
            assertEquals(ErrorCode.AUTH_SESSIONS_LIMIT_EXCEEDED, res.code)
        }

        @Test
        fun `happy path returns token pair and persists refresh token`() {
            val auth = mockk<Authentication> {
                every { principal } returns dummyUser
            }
            every { authManager.authenticate(
                match<UsernamePasswordAuthenticationToken> {
                    it.principal == "bob" && it.credentials == "raw‑pw"
                }
            ) } returns auth

            // No existing sessions
            every { refreshTokenRepository.findAllValidByUser(dummyUser) }
                .returns(emptyList())

            val accessToken = "access‑jwt"
            val refreshJwt  = "refresh‑jwt"

            every { jwtService.generateAccessToken(dummyUser) } returns Pair(accessToken, "jti‑123")
            every { jwtService.generateRefreshToken(dummyUser) } returns Pair(refreshJwt, "jti‑123")
            every { jwtService.extractExpiration(any()) } returns Date.from(Instant.now().plusSeconds(60))

            val savedSlot = slot<RefreshToken>()
            every { refreshTokenRepository.save(capture(savedSlot)) } returnsArgument 0

            val res = service.signInPassword("bob", "raw‑pw")
            assertEquals(accessToken, res.accessToken)
            assertEquals(refreshJwt, res.refreshToken)

            // Assert we saved a RefreshToken with correct fields
            val saved = savedSlot.captured
            assertEquals("jti‑123", saved.jti)
            assertFalse(saved.revoked)
            assertEquals(dummyUser, saved.user)
        }
    }

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Nested
    inner class TokenRefresh {
        private val fakeJwt = "fake.jwt.token"
        private val claims = mockk<Claims>()
        private lateinit var request: HttpServletRequest
        private lateinit var dummyRefreshToken: RefreshToken

        @BeforeEach
        fun commonHappyStubs() {
            dummyRefreshToken = RefreshToken(
                0,
                "someJti",
                Instant.now().plusSeconds(60),
                false,
                dummyUser
            )

            request = mockk {
                every { getHeader("Authorization") } returns "Bearer $fakeJwt"
            }

            every { jwtService.extractAllClaims(fakeJwt) } returns claims

            every { claims.subject } returns "bob"
            every { claims["jti"] } returns "someJti"

            every { userRepository.findByUsernameIgnoreCase("bob") } returns Optional.of(dummyUser)
            every { refreshTokenRepository.findByJti("someJti") } returns Optional.of(dummyRefreshToken)
        }

        fun badClaimsProvider() = listOf(
            Arguments.of(null, "someJti"),
            Arguments.of("alice", null),
            Arguments.of(null, null)
        ).stream()

        private fun assertInvalidCredentials(errorCode: ErrorCode = ErrorCode.AUTH_INVALID_CREDENTIALS, stubBlock: () -> Unit) {
            stubBlock()
            val ex = assertThrows<StatusCodeException> { service.refreshToken(request) }
            assertEquals(401, ex.statusCode)
            assertEquals(errorCode, ex.code)
        }

        @Test
        fun `missing Authorization header throws StatusCodeException 401`() = assertInvalidCredentials {
            request = mockk<HttpServletRequest> {
                every { getHeader("Authorization") } returns null
            }
        }

        @Test
        fun `invalid Authorization header format throws StatusCodeException 401`() = assertInvalidCredentials {
            request = mockk<HttpServletRequest> {
                every { getHeader("Authorization") } returns "Bearer-jwt"
            }
        }

        @ParameterizedTest
        @MethodSource("badClaimsProvider")
        fun `missing subject or jti throws StatusCodeException 401`(subject: String?, jti: String?) = assertInvalidCredentials {
            every { claims.subject } returns subject
            every { claims["jti"] } returns jti
        }

        @Test
        fun `user not found throws StatusCodeException 401`() = assertInvalidCredentials {
            every { userRepository.findByUsernameIgnoreCase("bob") } returns Optional.empty()
        }

        @Test
        fun `refresh token not found throws StatusCodeException 401`() = assertInvalidCredentials {
            every { refreshTokenRepository.findByJti("someJti") } returns Optional.empty()
        }

        @Test
        fun `revoked token throws StatusCodeException 401`() = assertInvalidCredentials {
            dummyRefreshToken.revoked = true
        }

        @Test
        fun `happy path returns token pair`() {
            every { jwtService.extractExpiration(any()) } returns Date.from(Instant.now().plusSeconds(60))
            every { jwtService.generateAccessToken(any()) } returns Pair("access", "token-1")
            every { jwtService.generateRefreshToken(any()) } returns Pair("refresh", "token-2")

            val savedSlot = slot<RefreshToken>()

            every { refreshTokenRepository.save(capture(savedSlot)) } returnsArgument 0

            val res = service.refreshToken(request)
            val savedToken = savedSlot.captured

            verify { refreshTokenRepository.save(match { it.jti == savedToken.jti }) }
            assertAll(
                { assertEquals(true, dummyRefreshToken.revoked) },
                { assertEquals("access", res.accessToken) },
                { assertEquals("refresh", res.refreshToken) }
            )
        }
    }

    @Nested
    inner class ChangePassword {
        private lateinit var oldPassword: String
        private lateinit var newPassword: String

        @BeforeEach
        fun commonHappyStubs() {
            oldPassword = "old-raw-password"
            newPassword = "new-raw-password"

            every { passwordEncoder.encode(any()) } returns "encoded"
            every { passwordEncoder.matches("old-raw-password", "current-user-password") } returns true
            every { passwordEncoder.matches("wrong-password", "current-user-password") } returns false
        }

        private fun assertInvalidInput(errorCode: Int, stubBlock: () -> Unit) {
            stubBlock()
            val ex = assertThrows<StatusCodeException> { service.changePassword(dummyUser, oldPassword, newPassword) }
            assertEquals(errorCode, ex.statusCode)
        }

        @Test
        fun `same old and new password throws StatusCodeException 400`() = assertInvalidInput(400) {
            newPassword = oldPassword
        }

        @Test
        fun `old password not matching throws StatusCodeException 403`() = assertInvalidInput(403) {
            oldPassword = "wrong-password"
        }

        @Test
        fun `happy path updates password`() {
            val dummyRefreshToken = RefreshToken(
                0,
                "someJti",
                Instant.now().plusSeconds(60),
                false,
                dummyUser
            )
            val dummyRefreshToken2 = RefreshToken(
                1,
                "someJti1",
                Instant.now().plusSeconds(60),
                false,
                dummyUser
            )
            val dummyTokens = listOf(dummyRefreshToken, dummyRefreshToken2)

            val savedUserSlot = slot<User>()
            every { userRepository.save(capture(savedUserSlot)) } returnsArgument 0

            val savedTokensSlot = slot<List<RefreshToken>>()
            every { refreshTokenRepository.findAllValidByUser(dummyUser) } returns dummyTokens
            every { refreshTokenRepository.saveAll(capture(savedTokensSlot)) } returnsArgument 0

            service.changePassword(dummyUser, oldPassword, newPassword)
            val savedUser = savedUserSlot.captured
            val savedTokens = savedTokensSlot.captured

            assertEquals(dummyUser.id, savedUser.id)
            assertTrue( savedTokens.all { it.revoked } )
            assertEquals(dummyTokens.count(), savedTokens.count())
        }
    }

    @Nested
    inner class SignOut {
        private var savedTokensSlot: CapturingSlot<List<RefreshToken>> = slot()

        @BeforeEach
        fun commonHappyStubs() {
            savedTokensSlot.clear()
            every { refreshTokenRepository.findAllValidByUser(dummyUser) } returns dummyTokens
            every { refreshTokenRepository.saveAll(capture(savedTokensSlot)) } returnsArgument 0
        }

        @Test
        fun `happy path revokes all user's refresh tokens`() {
            service.signOut(dummyUser)

            assertTrue(savedTokensSlot.captured.all { it.revoked })
        }
    }
}