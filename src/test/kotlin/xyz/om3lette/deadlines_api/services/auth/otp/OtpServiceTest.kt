package xyz.om3lette.deadlines_api.services.auth.otp

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import xyz.om3lette.deadlines_api.DomainObjectBuilder
import xyz.om3lette.deadlines_api.configs.properties.OtpProperties
import xyz.om3lette.deadlines_api.data.jwt.dto.TokenPair
import xyz.om3lette.deadlines_api.data.otp.response.OtpSignInResponse
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.data.user.repo.UserRepository
import xyz.om3lette.deadlines_api.data.integration.messengerAccount.repo.UserMessengerAccountRepository
import xyz.om3lette.deadlines_api.data.otp.dto.VerifiedOtp
import xyz.om3lette.deadlines_api.redisData.otp.enums.OtpChannel
import xyz.om3lette.deadlines_api.redisData.otp.enums.OtpPurpose
import xyz.om3lette.deadlines_api.redisData.otp.model.OtpPasswordCheck
import xyz.om3lette.deadlines_api.redisData.otp.model.OtpRegisterRequest
import xyz.om3lette.deadlines_api.redisData.otp.repo.OtpPasswordCheckRepository
import xyz.om3lette.deadlines_api.redisData.otp.repo.OtpRegisterRequestRepository
import xyz.om3lette.deadlines_api.redisData.otp.repo.OtpRepository
import xyz.om3lette.deadlines_api.services.auth.AuthSessionService
import xyz.om3lette.deadlines_api.services.auth.PasswordAuthService
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs

class OtpServiceTest {
    private val authSessionService: AuthSessionService = mockk()
    private val passwordAuthService: PasswordAuthService = mockk()
    private val userMessengerAccountRepository: UserMessengerAccountRepository = mockk()
    private val otpCodeHasher: OtpCodeHasher = mockk()
    private val otpVerificationService: OtpVerificationService = mockk()
    private val userRegistrationService: UserRegistrationService = mockk()
    private val userRepository: UserRepository = mockk()
    private val otpRepository: OtpRepository = mockk()
    private val otpRegisterRequestRepository: OtpRegisterRequestRepository = mockk()
    private val otpPasswordCheckRepository: OtpPasswordCheckRepository = mockk()
    private val otpService = OtpService(
        authSessionService,
        passwordAuthService,
        userMessengerAccountRepository,
        otpCodeHasher,
        otpVerificationService,
        userRegistrationService,
        userRepository,
        otpRepository,
        otpRegisterRequestRepository,
        otpPasswordCheckRepository,
        OtpProperties(hashSecret = "test-secret", maxAttempts = 3),
        emptyList()
    )

    private lateinit var user: User
    private val tokenPair = TokenPair("access", "refresh")

    @BeforeEach
    fun commonFixtures() {
        user = DomainObjectBuilder.userBob()
    }

    @Test
    fun `sign-in otp returns tokens when user has no password`() {
        val otpId = UUID.randomUUID()
        every { otpVerificationService.verifyAndConsume(otpId, "123456") } returns VerifiedOtp(
            OtpPurpose.SIGN_IN,
            user.username
        )
        every { userRepository.findByUsernameIgnoreCase(user.username) } returns Optional.of(user)
        every { authSessionService.issueSession(user) } returns tokenPair

        val result = assertIs<OtpSignInResponse.OK>(otpService.verifyOtpAndFulfillRequest(otpId, "123456"))

        assertEquals(tokenPair, result.tokenPair)
    }

    @Test
    fun `sign-in otp requires password when user has password`() {
        val otpId = UUID.randomUUID()
        user._password = "encoded-password"
        val savedPasswordCheck = slot<OtpPasswordCheck>()
        every { otpVerificationService.verifyAndConsume(otpId, "123456") } returns VerifiedOtp(
            OtpPurpose.SIGN_IN,
            user.username
        )
        every { userRepository.findByUsernameIgnoreCase(user.username) } returns Optional.of(user)
        every { otpPasswordCheckRepository.save(capture(savedPasswordCheck)) } returnsArgument 0

        otpService.verifyOtpAndFulfillRequest(otpId, "123456")

        assertEquals(user.username, savedPasswordCheck.captured.username)
    }

    @Test
    fun `registration otp creates user deletes registration request and returns tokens`() {
        val otpId = UUID.randomUUID()
        val registerRequest = OtpRegisterRequest(
            username = user.username,
            fullName = user.fullName,
            language = user.language,
            identifier = "123",
            channel = OtpChannel.TELEGRAM
        )
        every { otpVerificationService.verifyAndConsume(otpId, "123456") } returns VerifiedOtp(
            OtpPurpose.REGISTRATION,
            registerRequest.id.toString()
        )
        every { otpRegisterRequestRepository.findById(registerRequest.id) } returns Optional.of(registerRequest)
        every {
            userRegistrationService.registerExternalUser(
                registerRequest.username,
                registerRequest.fullName,
                registerRequest.channel,
                registerRequest.language,
                registerRequest.identifier
            )
        } returns user
        every { authSessionService.issueSession(user) } returns tokenPair
        every { otpRegisterRequestRepository.deleteById(registerRequest.id) } returns Unit

        val result = assertIs<OtpSignInResponse.OK>(otpService.verifyOtpAndFulfillRequest(otpId, "123456"))

        assertEquals(tokenPair, result.tokenPair)
        verify(exactly = 1) { otpRegisterRequestRepository.deleteById(registerRequest.id) }
    }
}
