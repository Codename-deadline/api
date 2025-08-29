package xyz.om3lette.deadlines_api.services.auth.otp

import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.CredentialsExpiredException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import xyz.om3lette.deadlines_api.data.user.model.User
import xyz.om3lette.deadlines_api.redisData.otp.model.Otp
import xyz.om3lette.deadlines_api.redisData.otp.repo.OtpRegisterRequestRepository
import xyz.om3lette.deadlines_api.redisData.otp.repo.OtpRepository

@Component
class OtpAuthProvider(
    private val userProvisioningService: UserProvisioningService,
    private val userDetailsService: UserDetailsService,
    private val passwordEncoder: PasswordEncoder,
    private val otpRepository: OtpRepository,
    private val otpRegisterRequestRepository: OtpRegisterRequestRepository
) : AuthenticationProvider {

    private val maxOtpAttempts: Int = 3

    companion object {
        val OTP_VERIFIED_AUTHORITY: GrantedAuthority = SimpleGrantedAuthority("OTP_VERIFIED")
    }

    private fun cleanupOtp(otp: Otp) {
        otpRepository.deleteById(otp.id)
        if (otp.registerRequestId != null) otpRegisterRequestRepository.deleteById(otp.registerRequestId)
    }

    override fun authenticate(authentication: Authentication): Authentication {
        val token = authentication as? OtpAuthenticationToken
            ?: throw IllegalArgumentException("Unsupported token")

        val otpId = token.identifier
        val code = token.credentials as? String ?: ""
        val otp = otpRepository.findById(otpId).orElseThrow { BadCredentialsException("") }

        if (!passwordEncoder.matches(code, otp.hashedCode)) {
            otp.attempts++
            if (otp.attempts >= maxOtpAttempts) {
                cleanupOtp(otp)
                throw CredentialsExpiredException("")
            }
            throw BadCredentialsException("")
        }

        // If otp is not issued for a sign-up it is used for a sign-in which guarantees that
        // user exists and username is supplied
        val username = if (otp.registerRequestId != null) {
            userProvisioningService.registerUserFromPending(otp.registerRequestId)
        } else otp.username!!
        cleanupOtp(otp)

        val userDetails: User = try {
            userDetailsService.loadUserByUsername(username) as User
        } catch (_: UsernameNotFoundException) {
            // If createUserFromPending created the user already, this shouldn't happen.
            throw BadCredentialsException("")
        }

        return if (userDetails.password.isNullOrBlank()) {
//          Full auth (no password set for user -> otp is enough to sign in)
            UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)
        } else {
//          Requires password verification
            UsernamePasswordAuthenticationToken(userDetails, null, listOf(OTP_VERIFIED_AUTHORITY))
        }
    }

    override fun supports(authentication: Class<*>): Boolean =
        OtpAuthenticationToken::class.java.isAssignableFrom(authentication)
}
