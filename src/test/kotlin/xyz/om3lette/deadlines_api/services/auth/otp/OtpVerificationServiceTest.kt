package xyz.om3lette.deadlines_api.services.auth.otp

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.CredentialsExpiredException
import xyz.om3lette.deadlines_api.configs.properties.OtpProperties
import xyz.om3lette.deadlines_api.data.otp.dto.VerifiedOtp
import xyz.om3lette.deadlines_api.redisData.otp.enums.OtpPurpose
import xyz.om3lette.deadlines_api.redisData.otp.model.Otp
import xyz.om3lette.deadlines_api.redisData.otp.repo.OtpRepository
import java.util.Optional
import java.util.UUID
import kotlin.test.assertEquals

class OtpVerificationServiceTest {
    private val otpCodeHasher: OtpCodeHasher = mockk()
    private val otpRepository: OtpRepository = mockk()
    private val otpProperties = OtpProperties(hashSecret = "test-secret", maxAttempts = 3)
    private val otpVerificationService = OtpVerificationService(
        otpCodeHasher,
        otpRepository,
        otpProperties
    )

    private lateinit var otp: Otp

    @BeforeEach
    fun commonStubs() {
        otp = Otp(
            id = UUID.randomUUID(),
            hashedCode = "hashed-code",
            purpose = OtpPurpose.SIGN_IN,
            context = "bob"
        )
        every { otpRepository.findById(otp.id) } returns Optional.of(otp)
    }

    @Test
    fun `valid code consumes otp and returns purpose with context`() {
        every { otpCodeHasher.matches("123456", otp.hashedCode) } returns true
        every { otpRepository.deleteById(otp.id) } returns Unit

        val result = otpVerificationService.verifyAndConsume(otp.id, "123456")

        assertEquals(VerifiedOtp(OtpPurpose.SIGN_IN, "bob"), result)
        verify(exactly = 1) { otpRepository.deleteById(otp.id) }
    }

    @Test
    fun `invalid code increments attempts and saves otp`() {
        every { otpCodeHasher.matches("000000", otp.hashedCode) } returns false
        every { otpRepository.save(otp) } returns otp

        assertThrows<BadCredentialsException> {
            otpVerificationService.verifyAndConsume(otp.id, "000000")
        }

        assertEquals(1, otp.attempts)
        verify(exactly = 1) { otpRepository.save(otp) }
    }

    @Test
    fun `invalid code at max attempts deletes otp`() {
        otp.attempts = 2
        every { otpCodeHasher.matches("000000", otp.hashedCode) } returns false
        every { otpRepository.deleteById(otp.id) } returns Unit

        assertThrows<CredentialsExpiredException> {
            otpVerificationService.verifyAndConsume(otp.id, "000000")
        }

        verify(exactly = 1) { otpRepository.deleteById(otp.id) }
    }
}
