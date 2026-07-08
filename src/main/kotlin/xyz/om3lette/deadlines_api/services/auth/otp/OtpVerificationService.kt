package xyz.om3lette.deadlines_api.services.auth.otp

import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.CredentialsExpiredException
import org.springframework.stereotype.Service
import xyz.om3lette.deadlines_api.configs.properties.OtpProperties
import xyz.om3lette.deadlines_api.data.otp.dto.VerifiedOtp
import xyz.om3lette.deadlines_api.redisData.otp.repo.OtpRepository
import java.util.UUID

@Service
class OtpVerificationService(
    private val otpCodeHasher: OtpCodeHasher,
    private val otpRepository: OtpRepository,
    private val otpProperties: OtpProperties
) {
    fun verifyAndConsume(otpId: UUID, code: String): VerifiedOtp {
        val otp = otpRepository.findById(otpId).orElseThrow { BadCredentialsException("") }

        if (!otpCodeHasher.matches(code, otp.hashedCode)) {
            ++otp.attempts
            if (otp.attempts >= otpProperties.maxAttempts) {
                otpRepository.deleteById(otp.id)
                throw CredentialsExpiredException("")
            }
            otpRepository.save(otp)
            throw BadCredentialsException("")
        }

        otpRepository.deleteById(otp.id)
        return VerifiedOtp(otp.purpose, otp.context)
    }
}
